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

import elide.tool.cli.ToolCommand

/**
 * TBD.
 */
internal sealed interface ToolError {
  /** Each tool error must carry a unique ID. */
  val id: String

  /** Each tool error must specify an exit code. */
  val exitCode: Int get() = 1

  /** Exception that caused this error. */
  val exception: Throwable? get() = null

  /** Message describing this error. */
  val errMessage: String? get() = null

  /** Whether the exception should halt execution. */
  val fatal: Boolean get() = true

  /** Command that this error relates to, as applicable. */
  val command: ToolCommand? get() = null
}
