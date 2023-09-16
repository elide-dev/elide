/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.cli

import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logger
import elide.runtime.Logging

/** Internal static tools and utilities used across the Elide CLI. */
internal object Statics {
  /** Main tool logger. */
  internal val logging: Logger by lazy {
    Logging.named("tool")
  }

  /** Server tool logger. */
  internal val serverLogger: Logger by lazy {
    Logging.named("tool:server")
  }

  /** Invocation args. */
  internal val args: AtomicReference<List<String>> = AtomicReference(emptyList())

  /** Main top-level tool. */
  val base: AtomicReference<ElideTool> = AtomicReference()
}
