package elide.server.cfg

import kotlin.test.*

/** Tests for asset-specific server configuration. */
class AssetConfigTest {
  @Test fun testAssetConfig() {
    val cfg = object : AssetConfig {}
    assertNotNull(cfg)
    assertNotNull(cfg.isEnabled)
    assertNotNull(cfg.prefix)
    assertTrue(cfg.isEnabled)
    assertTrue(cfg.etags ?: AssetConfig.DEFAULT_ENABLE_ETAGS)
    assertFalse(cfg.preferWeakEtags ?: AssetConfig.DEFAULT_PREFER_WEAK_ETAGS)

    val disabled = object : AssetConfig {
      override fun isEnabled(): Boolean = false
      override val prefix: String get() = "/_/somethingelse"
      override val etags: Boolean get() = false
      override val preferWeakEtags: Boolean get() = true
    }
    assertFalse(disabled.isEnabled)
    assertEquals(
      disabled.prefix,
      "/_/somethingelse",
    )
    assertFalse(disabled.etags)
    assertTrue(disabled.preferWeakEtags)
  }
}
