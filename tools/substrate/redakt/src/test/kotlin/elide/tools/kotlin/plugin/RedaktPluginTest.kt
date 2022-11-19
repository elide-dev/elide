package elide.tools.kotlin.plugin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Top-level tests for the Redakt plugin. */
class RedaktPluginTest : AbstractKotlinPluginTest() {
  /** Plugin constants should be expected values. */
  @Test fun testConstants() {
    assertEquals("redakt", RedaktPlugin().pluginId)
  }
}
