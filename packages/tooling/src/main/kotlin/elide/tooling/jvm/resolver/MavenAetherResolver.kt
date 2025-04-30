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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.AbstractRepositoryListener
import org.eclipse.aether.DefaultRepositoryCache
import org.eclipse.aether.RepositoryCache
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.locator.ServiceLocator
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import java.io.File
import java.lang.AutoCloseable
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import elide.runtime.Logging
import elide.tool.Classpath
import elide.tool.ClasspathProvider
import elide.tool.ClasspathSpec
import elide.tool.ClasspathsProvider
import elide.tool.MultiPathUsage
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage
import elide.tooling.project.manifest.ElidePackageManifest.MavenRepository

// Calculate the resolved Maven coordinate to use for a given dependency.
private fun MavenPackage.resolvedCoordinate(): String {
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
 * ## Maven Resolver
 */
public class MavenAetherResolver internal constructor () :
  DependencyResolver.MavenResolver,
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

  // Initialized Aether service locator.
  private lateinit var locator: ServiceLocator

  // Repository system; available after init.
  private lateinit var system: RepositorySystem

  // Repository system session; available after init.
  private lateinit var session: RepositorySystemSession

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
  private val registry = ConcurrentSkipListMap<String, MavenPackage>()

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
  private class ElideLocalRepositoryListener(private val state: ElideBuildState) : AbstractRepositoryListener() {
    // Nothing at this time.
  }

  // Listener for transport progress.
  private class ElideMavenTransferListener(private val state: ElideBuildState) : AbstractTransferListener() {
    // Nothing at this time.
  }

  // Initializes this resolver's internals at init-time.
  @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
  private fun prepareMavenResolver(state: ElideBuildState) {
    // Create a new Maven repository system
    logging.debug { "Initializing Maven resolver" }
    val mvnLocator = MavenRepositorySystemUtils.newServiceLocator()
    mvnLocator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, WagonTransporterFactory::class.java)
    mvnLocator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

    logging.trace { "Creating repo system" }
    val repoSystem = requireNotNull(mvnLocator.getService(RepositorySystem::class.java)) {
      "Failed to initialize Maven repository system: Repository system is null"
    }

    // Create a session for managing repository interactions
    logging.trace { "Creating repo session" }
    val repoSession = requireNotNull(MavenRepositorySystemUtils.newSession()) {
      "Failed to initialize Maven repository system: Repository session is null"
    }

    // prepare repository cache
    val repoCache = DefaultRepositoryCache()
    repoSession.cache = repoCache
    repoSession.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
    repoSession.updatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY
    repoSession.repositoryListener = ElideLocalRepositoryListener(state)
    repoSession.transferListener = ElideMavenTransferListener(state)

    // Set local repository
    try {
      logging.trace { "Creating local repo manager" }
      val localRepo = LocalRepository(state.localMaven().absolutePathString())
      repoSession.localRepositoryManager =
        requireNotNull(repoSystem.newLocalRepositoryManager(repoSession, localRepo)) {
          "Failed to initialize Maven repository system: Local repository manager is null"
        }

      locator = mvnLocator
      system = repoSystem
      session = repoSession
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
    val packages = registry.values.map {
      logging.trace { "- Adding Maven package: ${it.coordinate}" }
      val artifact = DefaultArtifact(it.resolvedCoordinate())
      Dependency(artifact, null)
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
      if (pkg.coordinate !in registry) {
        registry[pkg.coordinate] = pkg
      }
      registryByType.getOrDefault(suite, mutableListOf()).also {
        it.add(pkg)
        registryByType[suite] = it
      }
      registryPackageMap.getOrDefault(pkg, mutableListOf()).also {
        it.add(manifest)
      }
      when (val repo = pkg.repository) {
        // will resolve from default sources (central, et al)
        null -> {}

        else -> if (repo !in repositories) {
          error("Unknown Maven repository: '$repo', for package '${pkg.coordinate}'")
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
        "test",
        state.manifest,
        MultiPathUsage.TestCompile,
        testPackages,
      )
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
        val pkg = registry[coordinate] ?: registry.values.find {
          it.coordinate == coordinate || it.coordinate.startsWith(coordinate)
        }
        if (pkg == null) {
          val transitivePkg = MavenPackage(
            group = artifact.groupId,
            name = artifact.artifactId,
            version = artifact.version,
            coordinate = coordinate,
          )
          val resolved = ResolvedJarArtifact.of(artifact)
          packageArtifacts[transitivePkg] = resolved
          return@forEach
        }
        if (pkg in packageArtifacts) {
          // do we already have this at the same version?
          val existing = requireNotNull(packageArtifacts[pkg])
          if (existing.artifact.file == artifact.file) {
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

        if (errors.isNotEmpty()) {
          logging.error("Failed to resolve Maven dependencies because of one or more errors. Throwing.")
          throw MavenResolverErrors(errors)
        }
      })
    }
  }

  override suspend fun classpathProvider(spec: ClasspathSpec): ClasspathProvider? {
    check(initialized.value) { "Resolver must be initialized before resolving" }
    check(sealed.value) { "Resolver must be sealed before resolving" }
    check(resolved.value) { "Resolver must be fully resolved before assembling a classpath" }
    logging.debug { "Assembling classpath for spec: $spec" }

    return suites.find {
      spec.test(object : ClasspathSpec {
        override val name: String? get() = it.name
        override val usage: MultiPathUsage? get() = it.type
      })
    }?.let { suite ->
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
    } ?: error(
      "No classpath provider found for spec: $spec"
    )
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
