package elide.tool.cli.cmd.info

import elide.annotations.Singleton
import elide.tool.cli.cmd.AbstractSubcommand
import elide.tool.cli.ToolState
import picocli.CommandLine.Command

/** TBD. */
@Command(
  name = "info",
  description = ["Show info about the current app and environment"],
  mixinStandardHelpOptions = true,
)
@Singleton internal class ToolInfoCommand : AbstractSubcommand<ToolState>() {
  /** @inheritDoc */
  override fun invoke(context: ToolContext<ToolState>) {
    println("This command (`info`) is not implemented yet.")
  }
}
