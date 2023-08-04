/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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
      "/_/somethingelse"
    )
    assertFalse(disabled.etags)
    assertTrue(disabled.preferWeakEtags)
  }
}
