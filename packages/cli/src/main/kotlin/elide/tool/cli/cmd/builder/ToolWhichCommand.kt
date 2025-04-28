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

package elide.tool.cli.cmd.builder

import picocli.CommandLine
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.exec.which
import elide.tool.exec.whichAll

@CommandLine.Command(
  name = "which",
  description = [
    "Print the path to a tool binary",
    "",
    "Paths are resolved using the following sources, in order:",
    "- Local binaries in @|bold .dev/bin|@",
    "- Installed Node tools in @|bold node_modules/.bin|@",
    "- Installed Python tools in @|bold .dev/venv/bin|@",
    "- User binaries at @|bold ~/bin|@",
    "- System PATH",
    "",
    "Passing @|bold -a|-all|@ will print all paths instead of the first found.",
    "",
    "Examples:",
    "  elide which esbuild",
    "  elide which -a ruff",
  ],
  mixinStandardHelpOptions = true,
  customSynopsis = [
    "elide @|bold,fg(cyan) which|@ BIN",
    "   or: elide @|bold,fg(cyan) which|@ [OPTIONS] BIN",
    "",
  ],
)
class ToolWhichCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @CommandLine.Parameters(
    index = "0",
    arity = "0..1",
    description = ["Tool name to resolve"],
    paramLabel = "BIN",
    hideParamSyntax = true,
  )
  var tool: String? = null

  @CommandLine.Option(
    names = ["--all", "-a"],
    description = ["Print all paths to the tool binary"],
    defaultValue = "false",
  )
  var all: Boolean = false

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    return when (val toolName = tool) {
      null -> err("No tool name provided; please run `elide which <tool>`")
      else -> if (!all) when (val resolved = which(Path.of(toolName))) {
        null -> err("No tool found for name: $toolName")
        else -> success().also {
          output {
            append(resolved.absolutePathString())
          }
        }
      } else success().also {
        // we are printing all instances of the binary
        whichAll(Path.of(toolName)).collect {
          output {
            append(it.absolutePathString())
          }
        }
      }
    }
  }
}
