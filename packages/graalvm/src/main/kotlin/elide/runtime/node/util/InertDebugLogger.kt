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
package elide.runtime.node.util

import elide.runtime.intrinsics.js.node.util.DebugLogger
import elide.vm.annotations.Polyglot

// Disabled logger stub.
internal object InertDebugLogger : DebugLogger {
  @get:Polyglot override val loggerName: String get() = ""
  @get:Polyglot override val enabled: Boolean get() = false
  override fun emit(builder: StringBuilder): Unit = Unit
  override fun invoke(vararg arguments: Any?): Any? = null
}
