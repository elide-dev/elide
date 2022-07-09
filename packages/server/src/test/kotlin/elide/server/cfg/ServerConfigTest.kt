package elide.server.cfg

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** Tests for generic server-side configuration. */
class ServerConfigTest {
  @Test fun testServerConfigDefaults() {
    val config = ServerConfig()
    assertNotNull(config)
    assertNotNull(config.assets)
    val assetsDisabled = AssetConfig(enabled = false)
    config.assets = assetsDisabled
    assertFalse(config.assets.enabled)
  }
}
