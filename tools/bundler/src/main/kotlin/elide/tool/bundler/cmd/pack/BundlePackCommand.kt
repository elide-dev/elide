package elide.tool.bundler.cmd.pack

import picocli.CommandLine.Command
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.bundler.BundlerOperation
import elide.tool.bundler.cfg.ElideBundlerTool.ELIDE_TOOL_VERSION

/** Implements the `bundle pack` command. */
@Command(
  name = BundlePackCommand.CMD_NAME,
  description = ["Pack a VFS bundle"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
)
internal class BundlePackCommand : AbstractBundlerSubcommand() {
  internal companion object {
    internal const val CMD_NAME = "pack"
  }

  /** @inheritDoc */
  override fun invoke() = operation {
    logging.info("Bundle `pack` is not implemented yet")
  }
}
