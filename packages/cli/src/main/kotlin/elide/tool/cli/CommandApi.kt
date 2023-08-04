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

/**
 * # Command API
 *
 * Defines the external (publicly-accessible) API expected for command implementations.
 */
interface CommandApi {
  /** Observed exit code value; defaults to `0`. */
  val commandResult: AtomicReference<CommandResult>

  /** Shortcut for accessing the exit code for this command; only populated after execution. */
  val exitCode: Int get() = commandResult.get().exitCode
}
