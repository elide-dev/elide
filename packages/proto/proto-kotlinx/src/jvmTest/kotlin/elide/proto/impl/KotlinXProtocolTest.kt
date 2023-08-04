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

package elide.proto.impl

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import elide.proto.ElideProtocol.Dialect.*
import elide.proto.ElideProtocol.ImplementationLibrary.KOTLINX
import elide.proto.api.Protocol
import elide.proto.test.AbstractProtocolFacadeTest
import elide.testing.annotations.Test

/** Tests to load the Elide Protocol implementation backed by KotlinX Serialization. */
internal class KotlinXProtocolTest : AbstractProtocolFacadeTest<ElideKotlinXProtocol>() {
  /** @inheritDoc */
  override fun provide(): ElideKotlinXProtocol = Protocol.acquire(KOTLINX) as ElideKotlinXProtocol

  /** Test expected dialects for KotlinX implementation. */
  @Test override fun testExpectedDialects() {
    val dialects = provide().dialects()
    assertTrue(dialects.contains(JSON), "JSON should be supported by flat implementation")
  }

  /** Ensure that the declared protocol implementation is `KOTLINX`. */
  @Test override fun testExpectedLibrary() {
    assertEquals(KOTLINX, provide().engine(), "expected protocol implementation should be `KOTLINX`")
  }
}
