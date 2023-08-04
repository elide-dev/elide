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

package elide.server.runtime.jvm

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertTrue

/** Sanity tests for the [SecurityProviderConfigurator]. */
class SecurityProviderConfiguratorTest {
  @Test fun testInitSecurity() {
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
    assertTrue(
      SecurityProviderConfigurator.ready(),
      "security providers should show as `ready` after initialization"
    )
  }

  @Test fun testInitSecurityTwice() {
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
  }
}
