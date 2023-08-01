package elide.tool.cli

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import elide.testing.annotations.Test

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
