package elide.tool.cli.repl

import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.tool.cli.AbstractSubtoolTest
import kotlin.test.assertNotNull

/** Tests for the Elide tool `shell`/`repl` subcommand. */
@TestCase class ToolShellSubcommandTest : AbstractSubtoolTest() {
  @Inject internal lateinit var shell: ToolShellCommand

  override fun subcommand(): Runnable = shell

  @Test fun testEntrypoint() {
    assertNotNull(shell, "should be able to init and inject shell subcommand")
  }
}
