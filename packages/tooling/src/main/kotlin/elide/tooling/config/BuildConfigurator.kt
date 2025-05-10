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
import elide.exec.ActionScope
import elide.exec.TaskGraphBuilder
import elide.tooling.project.ElideConfiguredProject
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
    /** Whether to enable build caching. */
    public val caching: Boolean

    /** Whether to perform a dry run (skipping all actual actions). */
    public val dry: Boolean

    /** Whether to enable dependencies (installation-aware). */
    public val dependencies: Boolean

    /** Whether to enable check tasks, such as linters. */
    public val checks: Boolean

    /** @return Mutable form of these build settings. */
    public fun toMutable(): MutableBuildSettings
  }

  /**
   * ## Build Settings (Immutable)
   *
   * Build settings expressed in immutable form.
   */
  @Serializable @JvmRecord public data class ImmutableBuildSettings(
    override val caching: Boolean,
    override val dependencies: Boolean,
    override val dry: Boolean,
    override val checks: Boolean,
  ) : BuildSettings {
    override fun toMutable(): MutableBuildSettings = MutableBuildSettings(
      caching = caching,
      dependencies = dependencies,
      dry = dry,
      checks = checks,
    )
  }

  /**
   * ## Build Settings (Mutable)
   *
   * Build settings expressed in mutable form.
   */
  public data class MutableBuildSettings(
    override var caching: Boolean = true,
    override var dependencies: Boolean = true,
    override var dry: Boolean = false,
    override var checks: Boolean = true,
  ) : BuildSettings {
    public fun build(): BuildSettings = ImmutableBuildSettings(
      caching = caching,
      dependencies = dependencies,
      checks = checks,
      dry = dry,
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
  public interface BuildEvent : BuildNotify
  public interface BuildWork : BuildEvent
  public interface BuildWorker : BuildNotify
  public interface BuildTransfer : BuildNotify

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
  }

  public interface ProjectDirectories {
    public val projectRoot: Path
    public val workspaceRoot: Path get() = projectRoot
    public val devRoot: Path get() = projectRoot.resolve(".dev")
    public val cache: Path get() = devRoot.resolve("cache")
    public val dependencies: Path get() = devRoot.resolve("dependencies")
    public val artifacts: Path get() = devRoot.resolve("artifacts")
  }

  public interface ElideBuildState {
    public val beanContext: BeanContext
    public val project: ElideConfiguredProject
    public val console: BuildConsoleController
    public val events: BuildEventController
    public val layout: ProjectDirectories
    public val manifest: ElidePackageManifest
    public val resourcesPath: Path
    public val config: BuildConfiguration
  }

  public suspend fun contribute(state: ElideBuildState, config: BuildConfiguration)
}
