package elide.tool.cli.info

import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import kotlin.test.assertNotNull

/** Tests for the main CLI tool entrypoint. */
@TestCase class ToolInfoSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var info: ToolInfoCommand

  override fun subcommand(): Runnable = info

  @Test fun testEntrypoint() {
    assertNotNull(info, "should be able to init and inject info subcommand")
  }
}
