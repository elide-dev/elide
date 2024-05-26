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

package elide.tool.cli.cmd.lint

import picocli.CommandLine.Command
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.io.WorkdirManager
import elide.tool.project.ProjectManager

/** Interactive REPL entrypoint for Elide on the command-line. */
@Command(
  name = "lint",
  aliases = ["check"],
  description = ["%nRun polyglot linters on your code"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  hidden = true,
)
@Suppress("unused", "UnusedPrivateProperty")
@Singleton internal class ToolLintCommand @Inject constructor (
  private val projectManager: ProjectManager,
  private val workdir: WorkdirManager,
) : AbstractSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    output {
      append("Running linter (testing)")
      dev.elide.cli.bridge.CliNativeBridge.hello()
    }
    return success()
  }
}
