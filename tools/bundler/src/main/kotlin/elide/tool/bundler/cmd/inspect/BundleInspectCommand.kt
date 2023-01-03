package elide.tool.bundler.cmd.inspect

import picocli.CommandLine.Command
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.bundler.cfg.ElideBundlerTool.ELIDE_TOOL_VERSION

/** Implements the `bundle inspect` command. */
@Command(
  name = BundleInspectCommand.CMD_NAME,
  description = ["Inspect a VFS bundle"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
)
internal class BundleInspectCommand : AbstractBundlerSubcommand() {
  internal companion object {
    internal const val CMD_NAME = "inspect"
  }

  /** @inheritDoc */
  override fun invoke() = operation {
    logging.info("Bundle `inspect` is not implemented yet")
  }
}
