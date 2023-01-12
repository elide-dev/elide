package elide.tool.cli

import elide.testing.annotations.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

/** Common test utilities for Elide Tool sub-commands. */
abstract class AbstractSubtoolTest {
  /**
   * Return the sub-command implementation under test.
   */
  abstract fun subcommand(): Runnable

  protected open fun runCommand() {
    subcommand().run()
  }

  @Test open fun testRunPlain() {
    assertDoesNotThrow {
      runCommand()
    }
  }
}
