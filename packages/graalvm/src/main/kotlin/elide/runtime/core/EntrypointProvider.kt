/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.core

import org.graalvm.polyglot.Source
import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Singleton

/**
 * Shared registry that allows components to access the last entrypoint source evaluated by the runtime.
 *
 * An entry point must be registered by calling [record], after which it will be available through [acquire]. Consumers
 * should always check whether entry point information is [available] before attempting to use it.
 */
@Singleton public class EntrypointProvider {
  private val activeEntrypoint = AtomicReference<Source>()

  /** Whether an entrypoint source was previously [recorded][record]. */
  public val available: Boolean get() = activeEntrypoint.get() != null

  /**
   * Record a specific entrypoint [source], making it available for [acquire] calls. This method should only be called
   * by code that controls the evaluation of guest code, not by consumers of entrypoint information.
   */
  public fun record(source: Source) {
    activeEntrypoint.set(source)
  }

  /** Retrieve the current active entrypoint [recorded][record] by the runtime, or `null` if not available. */
  public fun acquire(): Source? {
    return activeEntrypoint.get()
  }
}
