package elide.tool.cli.cmd.info

import elide.annotations.Singleton
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import picocli.CommandLine.Command

/** TBD. */
@Command(
  name = "info",
  description = ["Show info about the current app and environment"],
  mixinStandardHelpOptions = true,
)
@Singleton internal class ToolInfoCommand : AbstractSubcommand<ToolState, CommandContext>() {
  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    println("This command (`info`) is not implemented yet.")
    return success()
  }
}
