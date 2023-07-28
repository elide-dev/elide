package elide.tool.cli.cmd.bundle

import elide.annotations.Singleton
import elide.tool.bundler.cmd.inspect.BundleInspectCommand
import elide.tool.bundler.cmd.pack.BundlePackCommand
import elide.tool.bundler.cmd.unpack.BundleUnpackCommand
import elide.tool.cli.ToolState
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import picocli.CommandLine

/** TBD. */
@CommandLine.Command(
  name = "bundle",
  description = ["Manipulate, pack, and unpack Elide VFS bundles"],
  mixinStandardHelpOptions = true,
  subcommands = [
    BundleInspectCommand::class,
    BundlePackCommand::class,
    BundleUnpackCommand::class,
  ],
)
@Singleton internal class ToolBundleCommand : AbstractSubcommand<ToolState, CommandContext>() {
  /** @inheritDoc */
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    println("Please pick a sub-command (see `elide bundle --help`) for options")
    return success()
  }
}
