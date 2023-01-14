@file:Suppress("RedundantVisibilityModifier")

package elide.tool.bundler.cmd.unpack

import picocli.CommandLine.Command
import elide.tool.bundler.AbstractBundlerSubcommand
import elide.tool.bundler.cfg.ElideBundlerTool.ELIDE_TOOL_VERSION

/** Implements the `bundle unpack` command. */
@Command(
  name = BundleUnpackCommand.CMD_NAME,
  description = ["Pack a VFS bundle"],
  mixinStandardHelpOptions = true,
  version = [ELIDE_TOOL_VERSION],
)
public class BundleUnpackCommand : AbstractBundlerSubcommand() {
  internal companion object {
    internal const val CMD_NAME = "unpack"
  }

  /** @inheritDoc */
  override fun invoke() = operation {
    logging.info("Bundle `unpack` is not implemented yet")
  }
}
