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
package elide.tooling.config

import io.micronaut.context.BeanContext
import java.nio.file.Path
import java.util.ServiceLoader
import elide.tooling.config.BuildConfigurator.BuildConfiguration
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * ## Build Configurators
 *
 * Deals with the discovery, instantiation, and "contribution" dispatch of [BuildConfigurator]-compliant instances from
 * the JVM service-loader protocol.
 *
 * Configurators are expected to be instantiable without parameters. Once created, configurators have a chance to
 * "contribute" to build configuration through their [BuildConfigurator.contribute] method.
 *
 * Build configurators impose no ordering semantics, and are expected to be executed in parallel during the preparatory
 * ("configuration") stage of a build. Thus, configurators should be idempotent, thread-safe, and independent of each
 * other.
 *
 * @see BuildConfigurator protocol for build configuration
 * @see TestConfigurator similar protocol for test configurators
 * @see TestConfigurators service driver for test configuration
 */
public object BuildConfigurators {
  @JvmStatic public fun collect(): Sequence<BuildConfigurator> {
    return ServiceLoader.load<BuildConfigurator>(BuildConfigurator::class.java).asSequence()
  }

  @JvmStatic
  public suspend fun contribute(
    beanContext: BeanContext,
    project: ElideConfiguredProject,
    from: Sequence<BuildConfigurator>,
    to: BuildConfiguration,
    extraConfigurator: BuildConfigurator? = null,
  ) {
    val eventController = object : BuildConfigurator.BuildEventController {
      override fun emit(event: BuildConfigurator.BuildEvent) {
        // nothing yet
      }
    }
    val layout = object : BuildConfigurator.ProjectDirectories {
      override val projectRoot: Path get() = to.projectRoot
    }
    val state = object : BuildConfigurator.ElideBuildState {
      override val beanContext: BeanContext get() = beanContext
      override val project: ElideConfiguredProject get() = project
      override val console: BuildConfigurator.BuildConsoleController get() = TODO("Not yet implemented")
      override val events: BuildConfigurator.BuildEventController get() = eventController
      override val manifest: ElidePackageManifest get() = project.manifest
      override val layout: BuildConfigurator.ProjectDirectories get() = layout
      override val resourcesPath: Path get() = project.resourcesPath
      override val config: BuildConfiguration get() = to
    }
    from.let {
      when (extraConfigurator) {
        null -> it
        else -> it + sequenceOf(extraConfigurator)
      }
    }.forEach {
      it.contribute(state, to)
    }
  }

  @JvmStatic public suspend fun contribute(
    beanContext: BeanContext,
    project: ElideConfiguredProject,
    to: BuildConfiguration,
    extraConfigurator: BuildConfigurator? = null,
  ) {
    contribute(beanContext, project, collect(), to, extraConfigurator)
  }
}
