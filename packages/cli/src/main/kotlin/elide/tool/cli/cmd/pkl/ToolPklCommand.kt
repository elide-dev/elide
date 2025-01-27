package elide.tool.cli.cmd.pkl

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.pkl.cli.commands.AnalyzeCommand
import org.pkl.cli.commands.EvalCommand
import org.pkl.cli.commands.RootCommand
import org.pkl.core.Release
import picocli.CommandLine.Command
import elide.tool.cli.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.ToolState

/** Pass-through to the Pkl command-line tools. */
@Command(
  name = "pkl",
  description = ["%nRun the Pkl command-line tools"],
  mixinStandardHelpOptions = false,
  synopsisHeading = "",
  customSynopsis = [],
  header = [],
)
internal class ToolPklCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Suppress("TooGenericExceptionCaught")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val version = Release.current().versionInfo()
    val helpLink = "${Release.current().documentation().homepage()}pkl-cli/index.html#usage"
    val args = Statics.args.get().let { args ->
      args.drop(args.indexOf("pkl") + 1)
    }

    return try {
      RootCommand("pkl", version, helpLink)
        .subcommands(
          EvalCommand(helpLink),
          AnalyzeCommand(helpLink),
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
