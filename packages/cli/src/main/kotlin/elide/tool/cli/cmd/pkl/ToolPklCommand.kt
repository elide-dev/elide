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

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.micronaut.core.annotation.Introspected
import org.pkl.cli.commands.*
import picocli.CommandLine
import elide.tool.cli.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.ToolState

/** Pass-through to the Pkl command-line tools. */
@CommandLine.Command(
  name = "pkl",
  description = ["Run the Pkl command-line tools"],
  mixinStandardHelpOptions = false,
  synopsisHeading = "",
  customSynopsis = [],
  header = [],
)
@Introspected
class ToolPklCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @CommandLine.Option(
    names = ["--help", "-h"],
    help = true,
    description = ["Show this help message and exit"],
  )
  var help: Boolean = false

  @Suppress("TooGenericExceptionCaught")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (help) {
      output {
        appendLine("Usage: elide pkl [OPTIONS] COMMAND [ARGS]...")
        appendLine()
        appendLine("Pkl is embedded in full within Elide's CLI. Use")
        appendLine("Pkl's command-line tools, but enabled with Elide's")
        appendLine("types, via `elide pkl ...`.")
        appendLine()
        appendLine("Available commands:")
        appendLine("  eval       Evaluate a Pkl expression")
        appendLine("  analyze    Analyze a Pkl package")
        appendLine("  download   Download a Pkl package")
        appendLine("  project    Manage Pkl projects")
        appendLine("  repl       Start a Pkl REPL")
        append("  test       Run tests for a Pkl package")
      }
      return success()
    }
    val args = Statics.args.let { args ->
      args.drop(args.indexOf("pkl") + 1)
    }

    return try {
      RootCommand()
        .subcommands(
          EvalCommand(),
          AnalyzeCommand(),
          DownloadPackageCommand(),
          ProjectCommand(),
          ReplCommand(),
          ServerCommand(),
          TestCommand(),
        )
        .main(args)
      success()

    } catch (err: RuntimeException) {
      output {
        appendLine("Failed to execute Pkl with error:")
        appendLine(err.stackTraceToString())
      }
      err("Failed to execute Pkl")
    }
  }
}
