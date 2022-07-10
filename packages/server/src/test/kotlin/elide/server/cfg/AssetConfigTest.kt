package elide.server.cfg

import kotlin.test.*

/** Tests for asset-specific server configuration. */
class AssetConfigTest {
  @Test fun testAssetConfig() {
    val cfg = AssetConfig()
    assertNotNull(cfg)
    assertNotNull(cfg.enabled)
    assertNotNull(cfg.prefix)
    assertTrue(cfg.enabled)

    // asset config should be mutable
    cfg.enabled = false
    assertFalse(cfg.enabled)
    cfg.prefix = "/_/somethingelse"
    assertEquals(
      cfg.prefix,
      "/_/somethingelse"
    )
  }
}
