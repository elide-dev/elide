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

package elide.runtime.gvm.internals.intrinsics.js.express

import org.graalvm.polyglot.Context

/** Internal abstraction used to manage VM context operations and JVM lifecycle utilities. */
internal interface ExpressContext {
  /** Increase the number of background tasks being performed that should prevent the JVM from exiting. */
  fun pin()
  
  /** Decrease the number of background tasks being performed that should prevent the JVM from exiting. */
  fun unpin()
  
  /** Execute a [block] of code after safely entering a VM [Context]. The context will be exited automatically. */
  fun <T> useGuest(block: Context.() -> T): T
}
