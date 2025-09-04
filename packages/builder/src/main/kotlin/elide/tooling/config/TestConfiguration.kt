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
import elide.exec.Action
import elide.exec.ActionScope
import elide.runtime.core.PolyglotContext
import elide.tooling.config.TestConfigurator.TestConfiguration
import elide.tooling.config.TestConfigurator.MutableTestSettings
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.ElideProject
import elide.tooling.project.load
import elide.tooling.registry.ResolverRegistry
import elide.tooling.testing.TestRegistry

/**
 * # Test Configuration
 *
 * Static utilities which assist in the construction of test configuration objects and related types.
 */
public object TestConfiguration {
  @JvmStatic public fun create(): TestConfiguration = create(
    Path.of(System.getProperty("user.dir")),
  )

  @JvmStatic
  public fun create(
    root: Path,
    scope: ActionScope? = null,
    registry: ResolverRegistry? = null,
    settings: MutableTestSettings? = null,
  ): TestConfiguration =
    CompositeTestConfiguration(
      scope ?: Action.scope(),
      registry ?: ResolverRegistry.create(),
      root,
      settings ?: MutableTestSettings(),
    )

  @JvmStatic
  public suspend fun ElideProject.configureTests(
    beanContext: BeanContext,
    guestContext: () -> PolyglotContext,
    with: TestConfiguration,
    registry: TestRegistry,
  ) {
    when (this) {
      is ElideConfiguredProject -> this
      else -> load()
    }.let {
      TestConfigurators.contribute(beanContext, guestContext, it, registry, with)
    }
  }

  @JvmStatic
  public suspend fun ElideProject.configureTests(
    ctx: BeanContext,
    guestContext: () -> PolyglotContext,
    registry: TestRegistry? = null,
  ): TestConfiguration {
    val effectiveRegistry = registry ?: requireNotNull(ctx.getBean(TestRegistry ::class.java)) {
      "Failed to configure tests: No available testing registry"
    }
    return create().also { configureTests(ctx, guestContext, it, effectiveRegistry) }
  }
}
