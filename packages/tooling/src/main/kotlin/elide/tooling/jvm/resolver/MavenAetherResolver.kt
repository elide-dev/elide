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
import org.eclipse.aether.SyncContext
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.WorkspaceReader
import org.eclipse.aether.repository.WorkspaceRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.locator.ServiceLocator
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import java.io.File
import java.lang.AutoCloseable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.io.path.absolutePathString
import kotlin.io.path.nameWithoutExtension
import elide.runtime.Logging
import elide.tooling.config.BuildConfigurator.ElideBuildState
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.manifest.ElidePackageManifest

// Calculate the resolved Maven coordinate to use for a given dependency.
private suspend fun ElidePackageManifest.MavenPackage.resolvedCoordinate(): String {
  return coordinate // @TODO resolve special versions
}

// Resolve the local dependencies path for Maven deps.
private fun ElideBuildState.localMaven(): Path {
  return layout.dependencies.resolve("m2")
}

/**
 * ## Maven Resolver
 */
public class MavenAetherResolver internal constructor () : DependencyResolver.MavenResolver, AutoCloseable {
  @JvmRecord private data class MavenDependencyResult(
    val result: DependencyResult,
  )

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

  // Synchronization context for Maven operations; available after init.
  private lateinit var sync: SyncContext

  // Whether this resolver has sealed to prevent further modification.
  private val sealed = atomic(false)

  // Prepared collect request for dependencies; available after seal.
  private lateinit var graph: CollectRequest

  // Registry of all witnessed Maven repositories.
  private val repositories = ConcurrentSkipListMap<String, ElidePackageManifest.MavenRepository>()

  // Registry of all witnessed Maven dependencies.
  private val registry = ConcurrentSkipListSet<ElidePackageManifest.MavenPackage>()

  // Maps packages to their originating projects.
  private val registryPackageMap = ConcurrentSkipListMap<ElidePackageManifest.MavenPackage, ElidePackageManifest>()

  // Maps packages to their originating projects.
  private val dependencyResults = ConcurrentLinkedQueue<MavenDependencyResult>()

  // Reader which checks in local project deps before resolving.
  private inner class ElideWorkspaceReader(private val state: ElideBuildState) : WorkspaceReader {
    private val workspaceRepo = WorkspaceRepository()

    override fun getRepository(): WorkspaceRepository = workspaceRepo

    private fun jarFileName(artifact: Artifact): String {
      return "${artifact.artifactId}-${artifact.version}.jar"
    }

    private fun artifactBasePath(artifact: Artifact, version: String? = artifact.version): Path {
      val base = state.localMaven()
      var resolved = base

      // `org/something/blah/`
      artifact.groupId.split('.').asSequence().plus(
        // `artifact/`
        artifact.artifactId
      ).let {
        when (version) {
          null -> it
          else -> it.plus(version)
        }
      }.forEach {
        resolved = resolved.resolve(it)
      }
      return resolved
    }

    override fun findArtifact(artifact: Artifact): File? {
      return artifactBasePath(artifact).resolve(jarFileName(artifact)).takeIf { Files.exists(it) }?.toFile()
    }

    override fun findVersions(artifact: Artifact): List<String> {
      return artifactBasePath(artifact, version = null).let { path ->
        if (!Files.exists(path)) {
          emptyList()
        } else {
          Files.list(path).filter { file ->
            Files.isDirectory(file)
          }.map { file ->
            file.nameWithoutExtension
          }.toList()
        }
      }
    }
  }

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
    repoSession.workspaceReader = ElideWorkspaceReader(state)
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

      // Sync context shared across users
      val syncSystem = repoSystem.newSyncContext(repoSession, true)

      locator = mvnLocator
      system = repoSystem
      session = repoSession
      sync = syncSystem
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
  private suspend fun configureMavenResolver() {
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
    val packages = registry.map {
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
    val repos = state.manifest.dependencies.maven.repositories
    if (packages.isNotEmpty()) {
      repos.forEach { repo ->
        if (repo.key !in repositories) {
          repo.value.name = repo.key
          repositories[repo.key] = repo.value
        }
      }
      packages.forEach { pkg ->
        if (pkg !in registry) {
          registry.add(pkg)
          registryPackageMap[pkg] = state.manifest
          when (val repo = pkg.repository) {
            // will resolve from default sources (central, et al)
            null -> {}

            else -> if (repo !in repositories) {
              error("Unknown Maven repository: '$repo', for package '${pkg.coordinate}'")
            }
          }
        }
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

  override suspend fun resolve(scope: CoroutineScope): Sequence<Job> = sequence {
    logging.debug { "Resolving Maven dependencies" }
    yield(scope.async {
      val dependencyRequest = DependencyRequest(graph, null)
      val dependencyResult = system.resolveDependencies(session, dependencyRequest)
      logging.debug { "Maven dependency resolution result: $dependencyResult" }
      dependencyResults.add(MavenDependencyResult(dependencyResult))
    })
  }

  override fun close() {
    if (initialized.value) {
      sync.close()
    }
  }

  public companion object {
    private const val DEFAULT_REPOSITORY_NAME = "central"

    // Maven Central repository.
    private val MAVEN_CENTRAL = ElidePackageManifest.MavenRepository(
      name = DEFAULT_REPOSITORY_NAME,
      url = "https://repo.maven.apache.org/maven2",
      description = "Maven Central Repository",
    )

    private val logging by lazy {
      Logging.of(MavenAetherResolver::class)
    }
  }
}
