package elide.tool.cli.info

import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import elide.tool.cli.cmd.info.ToolInfoCommand

/** Tests for the main CLI tool entrypoint. */
@TestCase class ToolInfoSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var info: ToolInfoCommand

  override fun subcommand(): Runnable = info

  @Test fun testEntrypoint() {
    assertNotNull(info, "should be able to init and inject info subcommand")
  }
}
