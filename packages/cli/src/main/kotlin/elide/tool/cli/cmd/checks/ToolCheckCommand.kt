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

package elide.tool.cli.cmd.checks

import com.github.ajalt.mordant.terminal.Terminal
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import jakarta.inject.Inject
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.project.ProjectManager

@Command(
  name = "check",
  aliases = ["lint", "format", "fmt"],
  description = ["Run configured checks or linters on the current project"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
class ToolCheckCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManager: ProjectManager

  // Terminal to use.
  private val terminal by lazy { Terminal() }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    output {
      append("Check runner is not implemented yet.")
    }
    return success()
  }
}
