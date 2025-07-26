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
package elide.tooling.builder

import io.micronaut.context.BeanContext
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import elide.exec.Action
import elide.exec.ActionScope
import elide.exec.ExecutionBinder
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.tooling.config.TestConfigurator.*
import elide.tooling.config.TestConfigurators
import elide.tooling.project.ElideProject
import elide.tooling.project.load
import elide.tooling.registry.ResolverRegistry

/**
 * # Test Driver
 *
 * Provides static utilities for configuring and executing test suites and related components of Elide projects.
 *
 * ## Usage
 *
 * A full end-to-end build invocation looks something like this:
 * ```kotlin
 * val project: ElideProject = /* ... */
 *
 * // must already be in a coroutine scope, or establish one for the build
 * coroutineScope {
 *   TestDriver.discoverTests(beanContext, project) {
 *     // bind build events here
 *   }
 * }
 * ```
 */
public object TestDriver {
  @JvmStatic
  public suspend fun CoroutineScope.configureTests(
    beanContext: BeanContext,
    project: ElideProject,
    registrar: TestingRegistrar? = null,
    binder: ExecutionBinder? = null,  // @TODO test binder
    resolvers: ResolverRegistry? = null,
    scope: ActionScope? = null,
  ): TestConfiguration {
    val effectiveRegistrar = registrar ?: requireNotNull(beanContext.getBean(TestingRegistrar::class.java)) {
      "Failed to resolve testing registrar from DI context"
    }
    val effectiveScope = scope ?: Action.scope()
    val effectiveResolvers = resolvers ?: ResolverRegistry.create()
    val settings = MutableTestSettings()

    return object: TestConfiguration {
      override val actionScope: ActionScope get() = effectiveScope
      override val resolvers: ResolverRegistry get() = effectiveResolvers
      override val projectRoot: Path get() = project.root
      override val settings: MutableTestSettings get() = settings
    }.also {
      TestConfigurators.contribute(beanContext, project.load(), effectiveRegistrar, it)
    }
  }

  @JvmStatic
  public suspend fun CoroutineScope.discoverTests(
    beanContext: BeanContext,
    project: ElideProject,
    registrar: TestingRegistrar? = null,
    binder: ExecutionBinder? = null,
  ) {
    configureTests(beanContext, project, registrar, binder)
  }
}
