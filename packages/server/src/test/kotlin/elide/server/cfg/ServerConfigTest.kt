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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/** Tests for generic server-side configuration. */
class ServerConfigTest {
  @Test fun testServerConfigDefaults() {
    val config = ServerConfig()
    assertNotNull(config)
    assertNotNull(config.assets)
    val assetsDisabled = object : AssetConfig {
      override fun isEnabled(): Boolean = false
    }
    config.assets = assetsDisabled
    assertFalse(config.assets.isEnabled)
  }
}
