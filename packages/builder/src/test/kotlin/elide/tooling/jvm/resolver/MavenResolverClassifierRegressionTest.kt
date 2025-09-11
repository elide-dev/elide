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
package elide.tooling.jvm.resolver

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertDoesNotThrow
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.transport.wagon.WagonTransporterFactory
import elide.tooling.config.BuildConfiguration
import elide.tooling.config.BuildConfigurator.BuildConfiguration as BuildConfigIf
import elide.tooling.config.BuildConfigurator.BuildEvent
import elide.tooling.config.BuildConfigurator.BuildEventController
import elide.tooling.config.BuildConfigurator.BuildWork
import elide.tooling.config.BuildConfigurator.BuildWorker
import elide.tooling.config.BuildConfigurator.BuildTransfer
import elide.tooling.config.BuildConfigurator.ProjectDirectories
import elide.tooling.config.BuildConfigurator.TaskState
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.ElideProjectInfo
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.ElidePackageManifest.DependencyResolution
import elide.tooling.project.manifest.ElidePackageManifest.MavenDependencies
import elide.tooling.project.manifest.ElidePackageManifest.MavenPackage

/**
 * Regression test for #1616: ensure that a Maven package with classifier provided, but an empty
 * coordinate string, does not cause the resolver to throw (i.e., it computes a safe coordinate
 * from fields and registers successfully).
 */
class MavenResolverClassifierRegressionTest {
  private fun testTempDir(): Path = Files.createDirectories(Path.of("build/test-target/maven-classifier-regression"))

  private fun repoSystem(): RepositorySystem {
    val locator: DefaultServiceLocator = MavenRepositorySystemUtils.newServiceLocator()
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, WagonTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    return locator.getService(RepositorySystem::class.java)
  }

  private fun session(): DefaultRepositorySystemSession = MavenRepositorySystemUtils.newSession()

  // Minimal no-op event controller for the resolver lifecycle.
  private object NoopEvents : BuildEventController {
    override fun emit(event: BuildEvent) {}
    override fun <T> emit(event: BuildEvent, ctx: T?) {}
    override fun <E : BuildEvent, X> bind(event: E, cbk: suspend E.(X) -> Unit) {}
  }

  // Minimal Elide build state to pass into registerPackagesFromManifest.
  private data class TestState(
    override val manifest: ElidePackageManifest,
    override val config: BuildConfigIf,
    override val resourcesPath: Path,
  ) : elide.tooling.config.BuildConfigurator.ElideBuildState {
    override val beanContext = io.micronaut.context.BeanContext.build().build()
    override val project: ElideConfiguredProject = ElideConfiguredProject.configure(
      project = ElideProjectInfo(root = resourcesPath, manifest = manifest),
      lockfile = null,
      sourceSets = object : elide.tooling.project.SourceSets {
        override fun contains(name: elide.tooling.project.SourceSetName) = false
        override fun get(name: elide.tooling.project.SourceSetName) = null
        override fun find(vararg types: elide.tooling.project.SourceSetType) = emptySequence<elide.tooling.project.SourceSet>()
        override fun find(vararg langs: elide.tooling.project.SourceSetLanguage) = emptySequence<elide.tooling.project.SourceSet>()
      },
      resourcesPath = resourcesPath,
    )
    override val console = object : elide.tooling.config.BuildConfigurator.BuildConsoleController {
      override fun onCurrentWork(work: BuildWork) {}
    }
    override val events: BuildEventController = NoopEvents
    override val layout: ProjectDirectories = object : ProjectDirectories {
      override val projectRoot: Path = resourcesPath
      override val dependencies: Path get() = projectRoot.resolve(".dev").resolve("dependencies").also { it.createDirectories() }
      override val artifacts: Path get() = projectRoot.resolve(".dev").resolve("artifacts").also { it.createDirectories() }
      override val devRoot: Path get() = projectRoot.resolve(".dev").also { it.createDirectories() }
    }
  }

  @Test
  fun testRegisterPackagesWithClassifierNoCoordinateDoesNotThrow() {
    val root = testTempDir()
    val cfg: BuildConfigIf = BuildConfiguration.create(root)

    // Manifest with Maven dependency that includes classifier but leaves coordinate empty.
    val pkg = MavenPackage(
      group = "com.example",
      name = "lib",
      version = "1.2.3",
      classifier = "native",
      repository = "",
      coordinate = "", // intentionally blank -> should be computed by resolver
    )
    val manifest = ElidePackageManifest(
      dependencies = DependencyResolution(
        maven = MavenDependencies(
          packages = listOf(pkg),
        )
      )
    )

    val state = TestState(
      manifest = manifest,
      config = cfg,
      resourcesPath = root,
    )

    val resolver = MavenAetherResolver(
      config = cfg,
      events = NoopEvents,
      system = repoSystem(),
      session = session(),
    )

    // Should not throw at any stage: registration or sealing
    assertDoesNotThrow {
      resolver.registerPackagesFromManifest(state)
      // We don’t actually resolve/download in this test; we only need registration + seal
      // to ensure the graph can be built with the computed coordinate.
      kotlinx.coroutines.runBlocking {
        resolver.seal()
      }
    }
  }
}

