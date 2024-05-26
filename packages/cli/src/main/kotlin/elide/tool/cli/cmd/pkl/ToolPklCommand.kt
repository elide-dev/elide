/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.tool.cli.cmd.pkl

import picocli.CommandLine.Command
import kotlin.test.fail
import elide.annotations.Singleton
import elide.tool.cli.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.ToolState

/** Access to Pkl entrypoint. */
@Command(
  name = "pkl",
  description = ["%nUse the Pkl language command-line tool"],
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  hidden = true,
)
@Singleton internal class ToolPklCommand : AbstractSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    return try {
      // val args = Statics.args.get()
      // val positionOfPkl = args.indexOfFirst { arg -> arg == "pkl" }
      // val pklWithArgs = args.slice((positionOfPkl + 1)..args.lastIndex)
      // org.pkl.cli.main(pklWithArgs.toTypedArray())
      success()
    } catch (exitErr: Exception) {
      fail(message = "Failed to run Pkl tool")
    }
  }
}
