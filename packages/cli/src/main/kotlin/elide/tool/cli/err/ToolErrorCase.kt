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

package elide.tool.cli.err

/**
 * TBD.
 */
internal sealed interface ToolErrorCase<T> : ToolError where T: Enum<T> {
  /** @return The error case itself. */
  @Suppress("UNCHECKED_CAST")
  val self: T get() = this as T

  /** @return ID of the error case, which is the name of the case. */
  override val id: String get() = self.name

  /** @return Throwable generated from this case which can be interpreted by the tool to formulate an exit code. */
  fun raise(target: Throwable? = null, doThrow: Boolean = false): Throwable {
    val exc = (target ?: AbstractToolError.forCase(this))
    if (doThrow) throw exc
    return exc
  }

  /** @return Exception to throw for this error case (without throwing). */
  fun asError() = raise(doThrow = false)
}
