/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded

import kotlinx.coroutines.test.runTest
import elide.embedded.ElideEmbedded.initialize
import elide.embedded.ElideEmbedded.capability
import elide.embedded.ElideEmbedded.configure
import elide.embedded.ElideEmbedded.teardown
import elide.embedded.api.Capability
import elide.embedded.api.ProtocolMode
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests Elide's embedded entrypoint. */
@TestCase class EmbeddedEntryTest : AbstractEmbeddedTest() {
  @Test fun testEntryNoArgs() = runTest {
    ElideEmbedded.entry(emptyArray())
  }

  @Test fun testInitializeEmptyLifecycle() {
    initialize(ProtocolMode.PROTOBUF)
    capability(Capability.BASELINE)
    configure(EMBEDDED_API_VERSION, null)  // configure with defaults
    teardown()
  }
}
