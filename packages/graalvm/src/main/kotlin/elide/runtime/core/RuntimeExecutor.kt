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
package elide.runtime.core

import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Singleton
import elide.runtime.exec.ContextAwareExecutor

@Singleton public class RuntimeExecutor {
  private val activeExecutor = AtomicReference<ContextAwareExecutor>()

  /** Whether an executor was previously [registered][register]. */
  public val available: Boolean get() = activeExecutor.get() != null

  /**
   * Record a [ContextAwareExecutor], making it available for [acquire] calls. This method should only be called
   * by code that controls the evaluation of guest code, not by consumers of entrypoint information.
   */
  public fun register(executor: ContextAwareExecutor) {
    activeExecutor.set(executor)
  }

  /** Retrieve the current active runtime executor, throwing an exception if not available. */
  public fun acquire(): ContextAwareExecutor {
    return activeExecutor.get() ?: error("No runtime executor has been registered")
  }
}
