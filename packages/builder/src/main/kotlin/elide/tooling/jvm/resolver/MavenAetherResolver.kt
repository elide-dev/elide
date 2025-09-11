/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
@file:Suppress("DEPRECATION")

package elide.tooling.jvm.resolver

import org.eclipse.aether.AbstractRepositoryListener
import org.eclipse.aether.DefaultRepositoryCache
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositoryCache
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transfer.TransferEvent.EventType
import org.eclipse.aether.util.artifact.SubArtifact
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import java.io.File
import java.lang.AutoCloseable
import java.nio.file.Path
import java.security.MessageDigest
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo
import elide.core.api.Symbolic
import elide.runtime.Logging
import elide.tooling.Classpath
import elide.tooling.ClasspathProvider
import elide.tooling.ClasspathSpec
import elide.tooling.ClasspathsProvider
import elide.tooling.MultiPathUsage
import elide.tooling.config.BuildConfigurator.*
import elide.tooling.deps.DependencyResolver
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.lockfile.Fingerprints
import elide.tooling.lockfile.LockfileStanza
import elide.tooling.project.ElideProject
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage
import elide.tooling.project.manifest.ElidePackageManifest.MavenRepository

// Calculate the resolved Maven coordinate to use for a given dependency.
@Suppress("UNUSED_PARAMETER")
private fun MavenPackage.resolvedCoordinate(usageType: MavenClassifier = MavenClassifier.Default): String {
  return coordinate // @TODO resolve special versions
}

// Resolve the local dependencies path for Maven deps.
private fun ElideBuildState.localMaven(): Path {
  return layout.dependencies.resolve("m2")
}

public class MavenResolverErrors internal constructor (public val errors: List<Throwable>) : RuntimeException(
  "Failed to resolve Maven dependencies; encountered ${errors.size} error(s). The first is shown.",
  errors.firstOrNull(),
)

/**
 * ## Maven Usage Type
 *
 * Enumerates types of libraries consumable via Maven.
 */
public enum class MavenClassifier(override val symbol: String): Symbolic<String> {
  Default(""),
  Docs("javadoc"),
  Sources("sources"),
}

/**
 * ## Maven Resolver
 *
 * Provides a [DependencyResolver.MavenResolver] implemented with Maven's own Aether-based resolver; this is the default
 * adapter for resolving Maven ecosystem dependencies.
 *
 * The resolver wires together a [RepositorySystem] and session from DI state, and configures each component to place
 * dependencies into a local Maven repository structure at the path `.dev/dependencies/m2`.
 *
 * Transitive dependencies are resolve by default, and various aspects of the resolver's behavior can be customized. The
 * resolver is aware of classpath usage types, and can assemble [ClasspathProvider] instances in order to make use of
 * the resolved dependencies.
 *
 * ### Usage
 *
 * Usage of this resolver is possible directly, but it is recommended that developers use the `BuildDriver` interface
 * instead, which will uniformly manage and resolve dependencies for a given Elide project.
 */
public class MavenAetherResolver internal constructor (
  @Suppress("unused") private val config: BuildConfiguration,
  private val events: BuildEventController,
  private val system: RepositorySystem,
  private val session: DefaultRepositorySystemSession,
) : DependencyResolver.MavenResolver,
    AutoCloseable,
    ClasspathsProvider {
  @JvmRecord private data class SourceSetSuite(
    val name: String,
    val type: MultiPathUsage,
  ) : Comparable<SourceSetSuite> {
    override fun compareTo(other: SourceSetSuite): Int {
      return name.compareTo(other.name)
    }
  }

  @JvmRecord private data class MavenDependencyResult(
    val result: DependencyResult,
  )

  private sealed interface ResolvedArtifact {
    val artifact: Artifact
    val files: List<File>
  }

  @JvmRecord private data class ResolvedJarArtifact private constructor (
    override val artifact: Artifact,
  ) : ResolvedArtifact {
    override val files: List<File> get() = listOf(artifact.file)

    companion object {
      @JvmStatic fun of(artifact: Artifact): ResolvedJarArtifact = ResolvedJarArtifact(artifact)
    }
  }

  // Whether this resolver has initialized yet.
  private val initialized = atomic(false)

  // Repository cache; available after init.
  private lateinit var cache: RepositoryCache

  // Local dependency repository; available after init.
  private lateinit var local: LocalRepository

  // Whether this resolver has sealed to prevent further modification.
  private val sealed = atomic(false)

  // Prepared collect request for dependencies; available after seal.
  private lateinit var graph: CollectRequest

  // Registry of all witnessed Maven repositories.
  private val repositories = ConcurrentSkipListMap<String, MavenRepository>()

  // Registry of all witnessed Maven dependencies.
  private val registry = ConcurrentSkipListMap<String, Pair<MavenPackage, MultiPathUsage>>()

  // Registry of Maven dependencies by suite type.
  private val registryByType = ConcurrentSkipListMap<SourceSetSuite, MutableList<MavenPackage>>()

  // Suites of Maven dependencies.
  private val suites = ConcurrentSkipListSet<SourceSetSuite>()

  // Maps packages to their originating projects.
  private val registryPackageMap = ConcurrentSkipListMap<MavenPackage, MutableList<ElidePackageManifest>>()

  // Maps packages to their originating projects.
  private val dependencyResults = ConcurrentLinkedQueue<MavenDependencyResult>()

  // Whether this resolver has totally finished resolving yet.
  private val resolved = atomic(false)

  // Artifacts held for each Maven package.
  private val packageArtifacts = ConcurrentSkipListMap<MavenPackage, ResolvedArtifact>()

  // Listener for events which emit from the repository system.
  private inner class ElideLocalRepositoryListener : AbstractRepositoryListener() {
    private fun toWorkStatus(event: RepositoryEvent): WorkStatus = when (event.type) {
      RepositoryEvent.EventType.ARTIFACT_DESCRIPTOR_INVALID,
      RepositoryEvent.EventType.ARTIFACT_DESCRIPTOR_MISSING,
      RepositoryEvent.EventType.METADATA_INVALID -> WorkStatus.FAILED
      RepositoryEvent.EventType.ARTIFACT_RESOLVING -> WorkStatus.STARTED
      RepositoryEvent.EventType.ARTIFACT_RESOLVED -> WorkStatus.SUCCEEDED
      RepositoryEvent.EventType.METADATA_RESOLVING -> WorkStatus.STARTED
      RepositoryEvent.EventType.METADATA_RESOLVED -> WorkStatus.SUCCEEDED
      RepositoryEvent.EventType.ARTIFACT_DOWNLOADING -> WorkStatus.STARTED
      RepositoryEvent.EventType.ARTIFACT_DOWNLOADED -> WorkStatus.SUCCEEDED
      RepositoryEvent.EventType.METADATA_DOWNLOADING -> WorkStatus.STARTED
      RepositoryEvent.EventType.METADATA_DOWNLOADED -> WorkStatus.SUCCEEDED
      RepositoryEvent.EventType.ARTIFACT_INSTALLING -> WorkStatus.STARTED
      RepositoryEvent.EventType.ARTIFACT_INSTALLED -> WorkStatus.SUCCEEDED
      RepositoryEvent.EventType.METADATA_INSTALLING -> WorkStatus.STARTED
      RepositoryEvent.EventType.METADATA_INSTALLED -> WorkStatus.SUCCEEDED
      else -> WorkStatus.UNKNOWN
    }

    private fun wrapRepositoryEvent(event: RepositoryEvent): TaskState = TaskState(
      name = event.type.name,
      status = toWorkStatus(event),
      context = event,
    )

    override fun metadataDownloading(event: RepositoryEvent) {
      events.emit(MetadataDownloading, wrapRepositoryEvent(event))
    }

    override fun metadataDownloaded(event: RepositoryEvent) {
      events.emit(MetadataDownloaded, wrapRepositoryEvent(event))
    }

    override fun artifactResolving(event: RepositoryEvent) {
      events.emit(ArtifactResolving, wrapRepositoryEvent(event))
    }

    override fun artifactResolved(event: RepositoryEvent) {
      events.emit(ArtifactResolved, wrapRepositoryEvent(event))
    }

    override fun artifactDownloading(event: RepositoryEvent) {
      events.emit(ArtifactDownloading, wrapRepositoryEvent(event))
    }

    override fun artifactDownloaded(event: RepositoryEvent) {
      events.emit(ArtifactDownloaded, wrapRepositoryEvent(event))
    }
  }

  // Listener for transport progress.
  private inner class ElideMavenTransferListener : AbstractTransferListener() {
    private fun toTransferStatue(event: TransferEvent): TransferStatus = when (event.type) {
      EventType.INITIATED -> TransferStatus.INITIATED
      EventType.STARTED -> TransferStatus.STARTED
      EventType.PROGRESSED -> TransferStatus.PROGRESSED
      EventType.SUCCEEDED -> TransferStatus.SUCCEEDED
      EventType.CORRUPTED -> TransferStatus.CORRUPTED
      EventType.FAILED -> TransferStatus.FAILED
      else -> TransferStatus.CORRUPTED
    }

    private fun wrapTransferEvent(event: TransferEvent): TransferState {
      return TransferState(
        name = event.resource.resourceName,
        size = event.resource.contentLength,
        repositoryId = event.resource.repositoryId,
        repositoryUrl = event.resource.repositoryUrl,
        bytesDone = event.transferredBytes,
        status = toTransferStatue(event),
      )
    }

    override fun transferInitiated(event: TransferEvent) {
      events.emit(TransferInitiated, wrapTransferEvent(event))
    }

    override fun transferStarted(event: TransferEvent) {
      events.emit(TransferStart, wrapTransferEvent(event))
    }

    override fun transferProgressed(event: TransferEvent) {
      events.emit(TransferProgress, wrapTransferEvent(event))
    }

    override fun transferSucceeded(event: TransferEvent) {
      events.emit(TransferSucceeded, wrapTransferEvent(event))
    }

    override fun transferCorrupted(event: TransferEvent) {
      events.emit(TransferCorrupted, wrapTransferEvent(event))
    }

    override fun transferFailed(event: TransferEvent) {
      events.emit(TransferFailed, wrapTransferEvent(event))
    }
  }

  // Initializes this resolver's internals at init-time.
  @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
  private fun prepareMavenResolver(state: ElideBuildState) {
    // Create a session for managing repository interactions
    logging.trace { "Creating repo session" }
    val repoSession = requireNotNull(session) {
      "Failed to initialize Maven repository system: Repository session is null"
    }

    // prepare repository cache
    val repoCache = DefaultRepositoryCache()
    repoSession.cache = repoCache
    repoSession.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
    repoSession.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY
    repoSession.repositoryListener = ElideLocalRepositoryListener()
    repoSession.transferListener = ElideMavenTransferListener()

    // Set local repository
    try {
      logging.trace { "Creating local repo manager" }
      val localRepo = LocalRepository(state.localMaven().absolutePathString())
      repoSession.localRepositoryManager =
        requireNotNull(system.newLocalRepositoryManager(repoSession, localRepo)) {
          "Failed to initialize Maven repository system: Local repository manager is null"
        }

      local = localRepo
      cache = repoCache
      initialized.value = true
      logging.trace { "Initialization of Maven resolver completed" }
    } catch (err: Throwable) {
      logging.error("Failed to initialize Maven resolver", err)
      error("Failed to initialize Maven resolver")
    }
  }

  // Prepares repositories and dependencies at seal-time.
  private fun configureMavenResolver() {
    if (repositories.isEmpty()) {
      // no repositories = no maven
      logging.debug { "No Maven repositories added; skipping Maven resolver configuration" }
      return
    }

    logging.debug { "Configuring Maven repositories" }
    val repos = repositories.map {
      logging.trace { "- Adding Maven repository: ${it.key} = ${it.value}" }

      RemoteRepository.Builder(
        it.key,
        "default",
        it.value.url,
      ).apply {
        // Nothing at this time.
      }.build()
    }.also {
      logging.debug { "Configured ${it.size} Maven repositories" }
    }

    logging.debug { "Configuring Maven packages" }
    val packages = registry.values.flatMap { (pkg, usage) ->
      logging.trace { "- Adding Maven package: ${pkg.coordinate}" }
      val coord = pkg.resolvedCoordinate()
      val artifact = DefaultArtifact(coord)
      buildList{
        // always fetch gradle metadata, if present
        // add(Dependency(SubArtifact(artifact, "", "module"), "metadata", true))

        // then main dependency
        add(Dependency(artifact, usage.scope))

        if (config.settings.docs) {
          add(Dependency(SubArtifact(artifact, "javadoc", "jar"), "metadata", true))
        }
        if (config.settings.sources) {
          add(Dependency(SubArtifact(artifact, "sources", "jar"), "metadata", true))
        }
      }
    }.also {
      logging.debug { "Configured ${it.size} Maven packages" }
    }
    graph = CollectRequest(packages, emptyList(), repos)
    logging.trace { "Maven resolver fully configured" }
  }

  // Initialize default repositories on first use.
  private fun initializeDefaults(manifest: ElidePackageManifest) {
    if (manifest.dependencies.maven.enableDefaultRepositories) {
      repositories[DEFAULT_REPOSITORY_NAME] = MAVEN_CENTRAL
    }
  }

  private fun registerPackages(
    name: String,
    manifest: ElidePackageManifest,
    usage: MultiPathUsage,
    packages: Iterable<MavenPackage>,
  ) {
    val suite = SourceSetSuite(
      type = usage,
      name = name,
    )
    suites.add(suite)

    packages.forEach { pkg ->
      // Use a resolved/safe coordinate to avoid null keys in registries.
      val key = pkg.resolvedCoordinate()
      if (key !in registry) {
        registry[key] = pkg to usage
      }
      registryByType.getOrDefault(suite, mutableListOf()).also {
        it.add(pkg)
        registryByType[suite] = it
      }
      registryPackageMap.getOrDefault(pkg, mutableListOf()).also {
        it.add(manifest)
      }
      when (val repo = pkg.repository?.ifBlank { null }) {
        // will resolve from default sources (central, et al)
        null -> {}

        else -> if (repo !in repositories) {
          error("Unknown Maven repository: '$repo', for package '${pkg.resolvedCoordinate()}'")
        }
      }
    }
  }

  // Register packages and repositories for the provided manifest.
  internal fun registerPackagesFromManifest(state: ElideBuildState) {
    synchronized(this) {
      require(!sealed.value) { "Cannot register packages after sealing" }
      require(!initialized.value) { "Cannot re-initialize resolver after it has been initialized" }

      if (!initialized.value) {
        initializeDefaults(state.manifest)
        prepareMavenResolver(state)
      }
    }

    val packages = state.manifest.dependencies.maven.packages
    val testPackages = state.manifest.dependencies.maven.testPackages
    val processors = state.manifest.dependencies.maven.processors
    val repos = state.manifest.dependencies.maven.repositories

    if (packages.isNotEmpty() || testPackages.isNotEmpty()) {
      repos.forEach { repo ->
        if (repo.key !in repositories) {
          repo.value.name = repo.key
          repositories[repo.key] = repo.value
        }
      }
      registerPackages(
        "main",
        state.manifest,
        MultiPathUsage.Compile,
        packages,
      )
      registerPackages(
        "main-runtime",
        state.manifest,
        MultiPathUsage.Runtime,
        packages,
      )
      registerPackages(
        "test",
        state.manifest,
        MultiPathUsage.TestCompile,
        testPackages,
      )
      registerPackages(
        "test-runtime",
        state.manifest,
        MultiPathUsage.TestRuntime,
        packages,
      )
      if (processors.isNotEmpty()) {
        registerPackages(
          "processors",
          state.manifest,
          MultiPathUsage.Processors,
          processors,
        )
      }
    }
  }

  override suspend fun seal() {
    synchronized(this) {
      require(!sealed.value) { "Resolver has already been sealed" }
      sealed.value = true
    }
    configureMavenResolver()
    logging.debug { "Maven resolver is now sealed" }
  }

  @Suppress("TooGenericExceptionCaught")
  private fun processDependencyResults(dependencyResult: DependencyResult, errors: MutableList<Throwable>) {
    val depClasspathReq = DependencyRequest(dependencyResult.root, null)
    depClasspathReq.setCollectRequest(graph)
    val rootNode = system.resolveDependencies(session, depClasspathReq).root

    val nlg = PreorderNodeListGenerator()
    rootNode.accept(nlg)
    val renderedDependencies = nlg.getDependencies(true)
    val renderedClasspath = nlg.classPath
    logging.trace { "Finalized dependencies: $renderedDependencies" }
    logging.debug { "Resolved classpath: $renderedClasspath" }

    renderedDependencies.forEach { dependency ->
      try {
        // @TODO cannot associate with source sets this early
        val artifact = dependency.artifact
        val coordinate = artifact.groupId + ":" + artifact.artifactId
        val pkg = registry[coordinate]?.first ?: registry.values.find { (pkg, _) ->
          pkg.coordinate == coordinate
        }?.first

        if (pkg == null) {
          val transitivePkg = MavenPackage(
            group = artifact.groupId,
            name = artifact.artifactId,
            version = artifact.version,
            classifier = artifact.classifier,
            coordinate = coordinate,
          )
          val resolved = ResolvedJarArtifact.of(artifact)
          packageArtifacts[transitivePkg] = resolved
          return@forEach
        }
        if (pkg in packageArtifacts) {
          // do we already have this at the same version?
          val existing = requireNotNull(packageArtifacts[pkg])
          if (existing.artifact.file == artifact.file || existing is ResolvedJarArtifact) {
            return@forEach // we already have this
          }
          error(
            "Duplicate Maven package artifact: '${pkg.coordinate}'. Packages are: " +
              "$pkg and $existing"
          )
        } else {
          packageArtifacts[pkg] = ResolvedJarArtifact.of(artifact)
        }
      } catch (err: Throwable) {
        errors.add(err)
      }
    }
  }

  override suspend fun resolve(scope: CoroutineScope): Sequence<Job> {
    check(initialized.value) { "Resolver must be initialized before resolving" }
    check(sealed.value) { "Resolver must be sealed before resolving" }
    check(!resolved.value) { "Resolver already resolved" }
    logging.debug { "Resolving Maven dependencies" }

    events.emit(ResolutionStart, TaskState(
      name = "maven",
      label = "Resolving Maven dependencies",
      total = 4,
      done = 1,
      status = WorkStatus.STARTED,
    ))

    return sequence {
      yield(scope.async {
        val dependencyRequest = DependencyRequest(graph, null)
        val dependencyResult = system.resolveDependencies(session, dependencyRequest)
        logging.debug { "Maven dependency resolution result: $dependencyResult" }
        var errors = mutableListOf<Throwable>()
        try {
          MavenDependencyResult(dependencyResult).also {
            dependencyResults.add(it)
            processDependencyResults(dependencyResult, errors)
          }
        } finally {
          resolved.value = true
        }
        events.emit(ResolutionFinished, TaskState(
          name = "maven",
          label = "Resolved Maven dependencies",
          total = 4,
          done = 4,
          status = WorkStatus.SUCCEEDED,
        ))

        if (errors.isNotEmpty()) {
          logging.error("Failed to resolve Maven dependencies because of one or more errors. Throwing.")
          throw MavenResolverErrors(errors)
        }
      })
    }
  }

  override suspend fun classpathProvider(spec: ClasspathSpec?): ClasspathProvider? {
    check(initialized.value) { "Resolver must be initialized before resolving" }
    check(sealed.value) { "Resolver must be sealed before resolving" }
    check(resolved.value) { "Resolver must be fully resolved before assembling a classpath" }
    logging.debug { "Assembling classpath for spec: $spec" }

    return when (spec) {
      null -> null
      else -> suites.find {
        spec.test(object : ClasspathSpec {
          override val name: String? get() = it.name
          override val usage: MultiPathUsage? get() = it.type
        })
      }
    }.let { _ ->
      ClasspathProvider {
        // assemble a class-path from all resolved artifacts for a given spec
//        Classpath.from((registryByType[suite] ?: emptyList()).flatMap { pkg ->
//          packageArtifacts[pkg]?.files?.map {
//            it.toPath()
//          } ?: emptyList()
//        })
        // @TODO don't return all artifacts, only those for the suite
        Classpath.from(packageArtifacts.flatMap { it.value.files }.map { it.toPath().absolute() })
      }
    }
  }

  override suspend fun contribute(root: Path, project: ElideProject?): ElideLockfile.Stanza? {
    val abs = root.absolute()
    check(initialized.value) { "Resolver must be initialized before resolving" }
    check(sealed.value) { "Resolver must be sealed before resolving" }
    check(resolved.value) { "Resolver must be fully resolved before assembling a classpath" }
    logging.debug { "Contributing Maven dependencies to lockfile" }
    val digest = MessageDigest.getInstance("SHA-1").let { digester ->
      registry.forEach { pkg ->
        digester.update(pkg.value.first.coordinate.toByteArray())
      }
      digester.digest()
    }

    var nextId = 0u
    fun idAssigner(): UInt {
      val id = nextId
      nextId++
      return id
    }

    val localIdMap = HashMap<MavenPackage, UInt>()
    val allHeld = packageArtifacts.mapNotNull {
      val coordinate = it.key.coordinate
      val pkg = it.key
      val artifact = it.value
      val classifier = it.value.artifact.classifier
      if (classifier == "sources" || classifier == "javadoc") {
        // always skip docs/sources w.r.t. lockfile calculations
        return@mapNotNull null
      }

      artifact.artifact.file.toPath().let { filePath ->
        idAssigner().let { assigned ->
          localIdMap[pkg] = assigned
          ElideLockfile.MavenArtifact(
            coordinate = coordinate,
            // @TODO use layout for this path
            artifact = filePath.relativeTo(abs.resolve(".dev").resolve("dependencies")).toString(),
            fingerprint = Fingerprints.forFile(filePath),
            id = assigned,
          )
        }
      }
    }

    val usageMap = HashMap<UInt, SortedSet<MultiPathUsage>>()
    allHeld.map { artifact ->
      val pkg = registry[artifact.coordinate]?.first
      val usages = if (pkg == null) listOf(MultiPathUsage.Compile) else {
        registryByType.filter {
          pkg in it.value
        }.flatMap {
          it.key.type.expand()
        }
      }.toSortedSet()

      val assigned = requireNotNull(pkg?.let { localIdMap[it] } ?: artifact.id) {
        "Failed to locate local ID for lockfile payload: $pkg"
      }
      usageMap.computeIfAbsent(assigned) {
        TreeSet()
      }.also {
        it.addAll(usages)
      }
    }
    return when (packageArtifacts.isEmpty()) {
      true -> null
      else -> ElideLockfile.StanzaData(
        identifier = LockfileStanza.MAVEN,
        fingerprint = Fingerprints.forBytes(digest),
        contributedBy = "Maven integration",
        inputs = setOf(),
        state = ElideLockfile.MavenLockfile(
          classpath = allHeld,
          usage = usageMap.map { entry ->
            ElideLockfile.MavenUsage(
              id = entry.key,
              types = entry.value.map { usageType ->
                when (usageType) {
                  MultiPathUsage.Compile -> ElideLockfile.MavenUsageType.COMPILE
                  MultiPathUsage.TestCompile -> ElideLockfile.MavenUsageType.TEST
                  MultiPathUsage.Runtime -> ElideLockfile.MavenUsageType.RUNTIME
                  MultiPathUsage.Processors -> ElideLockfile.MavenUsageType.PROCESSORS
                  MultiPathUsage.TestProcessors -> ElideLockfile.MavenUsageType.TEST_PROCESSORS
                  MultiPathUsage.TestRuntime -> ElideLockfile.MavenUsageType.TEST_RUNTIME
                }
              }.toSortedSet(),
            )
          }
        ),
      )
    }
  }

  override fun close() {
    // @TODO
  }

  public companion object {
    private const val DEFAULT_REPOSITORY_NAME = "central"

    // Maven Central repository.
    private val MAVEN_CENTRAL = MavenRepository(
      name = DEFAULT_REPOSITORY_NAME,
      url = "https://repo1.maven.org/maven2/",
      description = "Maven Central Repository",
    )

    private val logging by lazy {
      Logging.of(MavenAetherResolver::class)
    }
  }
}
