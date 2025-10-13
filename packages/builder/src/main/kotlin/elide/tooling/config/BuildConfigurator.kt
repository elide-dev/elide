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
@file:Suppress("UnstableApiUsage")

package elide.tooling.config

import io.micronaut.context.BeanContext
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import elide.exec.ActionScope
import elide.exec.TaskGraphBuilder
import elide.tooling.BuildMode
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.ElideProject
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec.ManifestBuildState
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.registry.ResolverRegistry

/**
 * # Build Configurator
 *
 * Build configurators implement some independently-calculable suite of configuration functionality for Elide project
 * builds; for example, a configurator may be responsible for configuring build caching, or for compiling sources for a
 * given language.
 *
 * Build configurators are loaded via the JVM `ServiceLoader` mechanism, and are expected to be registered at build-time
 * in a no-param instantiable form. When configuring user builds, state is provided to the configurator, and it has a
 * chance to contribute build information, create graph tasks, and mount other configuration, such as dependency
 * resolver implementations.
 *
 * Project builds take place before execution (running, testing, and so on). Tests and execution are configured via
 * other means; see [TestConfigurator] for testing, and the CLI module for execution.
 *
 * @see BuildConfigurators build configurator loading and processing
 * @see TestConfigurator similar protocol but for project test configuration
 */
public fun interface BuildConfigurator : ProjectConfigurator {
  /**
   * ## Build Settings
   */
  public interface BuildSettings {
    /** Whether to preserve build ephemera for debugging. */
    public val preserve: Boolean

    /** Whether to emit extra logging. */
    public val verbose: Boolean

    /** Whether the user is building a release. */
    public val release: Boolean

    /** Whether the user is building a debug-enabled release. */
    public val debug: Boolean

    /** Whether to enable build caching. */
    public val caching: Boolean

    /** Whether to perform a dry run (skipping all actual actions). */
    public val dry: Boolean

    /** Whether to enable dependencies (installation-aware). */
    public val dependencies: Boolean

    /** Whether to download source dependencies (installation-aware). */
    public val sources: Boolean

    /** Whether to download doc dependencies (installation-aware). */
    public val docs: Boolean

    /** Whether to enable check tasks, such as linters. */
    public val checks: Boolean

    /** Operating mode for the build. */
    public val buildMode: BuildMode

    /** Whether to publish and/or deploy. */
    public val deploy: Boolean

    /** @return Mutable form of these build settings. */
    public fun toMutable(): MutableBuildSettings
  }

  /**
   * ## Build Settings (Immutable)
   *
   * Build settings expressed in immutable form.
   */
  @Serializable @JvmRecord public data class ImmutableBuildSettings(
    override val preserve: Boolean,
    override val release: Boolean,
    override val debug: Boolean,
    override val verbose: Boolean,
    override val caching: Boolean,
    override val dependencies: Boolean,
    override val dry: Boolean,
    override val checks: Boolean,
    override val buildMode: BuildMode,
    override val deploy: Boolean,
    override val sources: Boolean,
    override val docs: Boolean,
  ) : BuildSettings {
    override fun toMutable(): MutableBuildSettings = MutableBuildSettings(
      caching = caching,
      dependencies = dependencies,
      preserve = preserve,
      release = release,
      dry = dry,
      checks = checks,
      verbose = verbose,
      debug = debug,
      buildMode = buildMode,
      deploy = deploy,
      sources = sources,
      docs = docs,
    )
  }

  /**
   * ## Build Settings (Mutable)
   *
   * Build settings expressed in mutable form.
   */
  public data class MutableBuildSettings(
    override var caching: Boolean = true,
    override var preserve: Boolean = true,
    override var verbose: Boolean = false,
    override var release: Boolean = false,
    override var debug: Boolean = false,
    override var dependencies: Boolean = true,
    override var dry: Boolean = false,
    override var checks: Boolean = true,
    override var buildMode: BuildMode = BuildMode.default(),
    override var deploy: Boolean = false,
    override var sources: Boolean = false,
    override var docs: Boolean = false,
  ) : BuildSettings {
    public fun build(): BuildSettings = ImmutableBuildSettings(
      caching = caching,
      preserve = preserve,
      dependencies = dependencies,
      checks = checks,
      dry = dry,
      release = release,
      verbose = verbose,
      debug = debug,
      buildMode = buildMode,
      deploy = deploy,
      sources = sources,
      docs = docs,
    )

    override fun toMutable(): MutableBuildSettings = this
  }

  /**
   * ## Build Configuration
   *
   * State API provided to [BuildConfigurator] instances.
   */
  public interface BuildConfiguration {
    public val actionScope: ActionScope
    public val taskGraph: TaskGraphBuilder
    public val resolvers: ResolverRegistry
    public val projectRoot: Path
    public val settings: MutableBuildSettings
  }

  public sealed interface BuildNotify
  public sealed interface BuildEvent : BuildNotify
  public interface BuildWork : BuildEvent
  public interface BuildWorker : BuildNotify
  public interface BuildTransfer : BuildNotify

  public sealed interface TransferEvent : BuildEvent
  public data object TransferInitiated : TransferEvent
  public data object TransferStart : TransferEvent
  public data object TransferProgress : TransferEvent
  public data object TransferSucceeded : TransferEvent
  public data object TransferCorrupted : TransferEvent
  public data object TransferFailed : TransferEvent

  public data object ResolutionStart : BuildEvent
  public data object ResolutionProgress : BuildEvent
  public data object ResolutionFailed : BuildEvent
  public data object ResolutionFinished : BuildEvent
  public data object AcquisitionStart : BuildEvent
  public data object AcquisitionFinished : BuildEvent

  public sealed interface RepositoryEvent : BuildEvent
  public data object MetadataDownloading : TransferEvent
  public data object MetadataDownloaded : TransferEvent
  public data object ArtifactResolving : TransferEvent
  public data object ArtifactResolved : TransferEvent
  public data object ArtifactDownloading : TransferEvent
  public data object ArtifactDownloaded : TransferEvent

  public enum class TransferStatus {
    INITIATED,
    STARTED,
    PROGRESSED,
    CORRUPTED,
    SUCCEEDED,
    FAILED
  }

  public enum class WorkStatus {
    INITIATED,
    STARTED,
    PROGRESSED,
    SUCCEEDED,
    FAILED,
    UNKNOWN,
  }

  @JvmRecord public data class TransferState(
    public val name: String? = null,
    public val size: Long? = null,
    public val bytesDone: Long? = null,
    public val repositoryId: String? = null,
    public val repositoryUrl: String? = null,
    public val status: TransferStatus = TransferStatus.INITIATED,
  )

  @JvmRecord public data class TaskState(
    public val name: String? = null,
    public val label: String? = null,
    public val total: Long? = null,
    public val done: Long? = null,
    public val status: WorkStatus = WorkStatus.INITIATED,
    public val context: Any? = null,
  )

  public interface BuildConsoleController {
    public fun onCurrentWork(work: BuildWork)
  }

  public interface BuildEventListener {
    public fun onEventBegin(event: BuildEvent) {}
    public fun onEventEnd(event: BuildEvent) {}
    public fun onWorkerStart(id: BuildWorker, task: String) {}
    public fun onTransferStart(transfer: BuildTransfer, event: BuildEvent) {}
    public fun onProgress(event: BuildEvent) {}
    public fun onProgress(worker: BuildWorker, event: BuildEvent) {}
    public fun onProgress(transfer: BuildTransfer, event: BuildEvent) {}
    public fun onWorkerEnd(id: BuildWorker) {}
    public fun onTransferEnd(transfer: BuildTransfer) {}
    public fun onError(event: BuildEvent) {}
    public fun onWorkerError(worker: BuildWorker, event: BuildEvent) {}
    public fun onTransferError(transfer: BuildTransfer, event: BuildEvent) {}
  }

  public interface BuildEventController {
    public fun emit(event: BuildEvent)
    public fun <T> emit(event: BuildEvent, ctx: T? = null)
    public fun <E: BuildEvent, X> bind(event: E, cbk: suspend E.(X) -> Unit)
  }

  public interface ProjectDirectories {
    public val projectRoot: Path
    public val workspaceRoot: Path get() = projectRoot
    public val devRoot: Path get() = projectRoot.resolve(".dev")
    public val cache: Path get() = devRoot.resolve("cache")
    public val dependencies: Path get() = devRoot.resolve("dependencies")
    public val artifacts: Path get() = devRoot.resolve("artifacts")

    public companion object {
      @JvmStatic public fun forProject(project: ElideProject): ProjectDirectories = object : ProjectDirectories {
        override val projectRoot: Path get() = project.root
      }
    }
  }

  public interface ElideBuildState {
    public val debug: Boolean
    public val release: Boolean
    public val beanContext: BeanContext
    public val project: ElideConfiguredProject
    public val console: BuildConsoleController
    public val events: BuildEventController
    public val layout: ProjectDirectories
    public val manifest: ElidePackageManifest
    public val resourcesPath: Path
    public val config: BuildConfiguration

    public fun forManifest(): ManifestBuildState = object: ManifestBuildState {
      override val isDebug: Boolean get() = debug
      override val isRelease: Boolean get() = release
    }
  }

  /**
   * Contribute configurations to the current build.
   *
   * This method is called during the build configuration phase, and allows the configurator to configure various
   * aspects of Elide's build infrastructure.
   *
   * @param state Current build state, which provides access to the project, console, event controller, and other
   *   state.
   * @param config Current build configuration.
   */
  public suspend fun contribute(state: ElideBuildState, config: BuildConfiguration)

  /**
   * Indicate the build configurators which must run before this one.
   *
   * @return List of configurator classes; defaults to an empty list.
   */
  public fun dependsOn(): List<KClass<out BuildConfigurator>> = emptyList()
}
