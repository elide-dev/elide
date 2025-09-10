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
import elide.runtime.core.PolyglotContext
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.testing.TestRegistry

/**
 * ## Test Configurators
 *
 * Deals with the discovery, instantiation, and "contribution" dispatch of [TestConfigurator]-compliant instances from
 * the JVM service-loader protocol.
 *
 * Configurators are expected to be instantiable without parameters. Once created, configurators have a chance to
 * "contribute" to test through their [TestConfigurator.contribute] method.
 *
 * Test configurators impose no ordering semantics, and are expected to be executed in parallel during the preparatory
 * ("configuration") stage of a build and/or test run. Thus, configurators should be idempotent, thread-safe, and
 * independent of each other.
 *
 * Test configurators are guaranteed to be executed before relevant [BuildConfigurators].
 *
 * @see TestConfigurator protocol for test configurators
 * @see BuildConfigurator similar protocol for build configuration
 * @see BuildConfigurators service driver for build configuration
 */
public object TestConfigurators {
  @JvmStatic public fun collect(): Sequence<TestConfigurator> {
    return ServiceLoader.load<TestConfigurator>(TestConfigurator::class.java).asSequence()
  }

  @JvmStatic
  public suspend fun contribute(
    beanContext: BeanContext,
    guestContext: () -> PolyglotContext,
    project: ElideConfiguredProject,
    registry: TestRegistry,
    from: Sequence<TestConfigurator>,
    to: TestConfigurator.TestConfiguration,
    extraConfigurator: TestConfigurator? = null,
  ) {
    val layout = object : BuildConfigurator.ProjectDirectories {
      override val projectRoot: Path get() = to.projectRoot
    }
    val state = object : TestConfigurator.ElideTestState {
      override val beanContext: BeanContext get() = beanContext
      override val project: ElideConfiguredProject get() = project
      override val events: TestConfigurator.TestEventController get() = TODO("Not yet implemented")
      override val manifest: ElidePackageManifest get() = project.manifest
      override val layout: BuildConfigurator.ProjectDirectories get() = layout
      override val resourcesPath: Path get() = project.resourcesPath
      override val registry: TestRegistry get() = registry
      override val guestContextProvider: () -> PolyglotContext = guestContext
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
    guestContext: () -> PolyglotContext,
    project: ElideConfiguredProject,
    registry: TestRegistry,
    to: TestConfigurator.TestConfiguration,
    extraConfigurator: TestConfigurator? = null,
  ) {
    contribute(beanContext, guestContext, project, registry, collect(), to, extraConfigurator)
  }
}
