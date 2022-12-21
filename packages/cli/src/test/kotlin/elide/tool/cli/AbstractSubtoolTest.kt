package elide.tool.cli

import elide.testing.annotations.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow

/** Common test utilities for Elide Tool sub-commands. */
abstract class AbstractSubtoolTest {
  /**
   * Return the sub-command implementation under test.
   */
  abstract fun subcommand(): Runnable

  @Test fun testRunPlain() {
    assertDoesNotThrow {
      subcommand().run()
    }
  }
}
