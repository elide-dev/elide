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

/** Enumerates available sub-commands for the Elide command-line tool. */
@Suppress("unused") internal enum class ToolCommand (internal val commandName: String) {
  /** Root tool command (i.e. no sub-command). */
  ROOT("elide"),

  /** Tool to gather info about an application or development environment. */
  INFO("info"),

  /** Tool to run code in a guest language VM. */
  RUN("run")
}
