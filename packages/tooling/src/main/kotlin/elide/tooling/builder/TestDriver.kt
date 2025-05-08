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
import kotlinx.coroutines.CoroutineScope
import elide.exec.ExecutionBinder
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.tooling.project.ElideProject

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
 *
 */
public object TestDriver {
  @JvmStatic
  public suspend fun CoroutineScope.discoverTests(
    beanContext: BeanContext,
    project: ElideProject,
    registrar: TestingRegistrar? = null,
    binder: ExecutionBinder? = null,
  ) {

  }
}
