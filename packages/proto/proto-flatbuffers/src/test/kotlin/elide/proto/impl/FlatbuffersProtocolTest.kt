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
import elide.proto.api.Protocol
import elide.proto.test.AbstractProtocolFacadeTest
import elide.testing.annotations.Test
import elide.proto.ElideProtocol.ImplementationLibrary.FLATBUFFERS as FLAT

/** Tests to load the Elide Protocol implementation backed by Flatbuffers. */
internal class FlatbuffersProtocolTest : AbstractProtocolFacadeTest<ElideFlatbuffersProtocol>() {
  /** @inheritDoc */
  override fun provide(): ElideFlatbuffersProtocol = Protocol.acquire(FLAT) as ElideFlatbuffersProtocol

  /** Test expected dialects for Flatbuffers. */
  @Test override fun testExpectedDialects() {
    val dialects = provide().dialects()
    assertTrue(dialects.contains(JSON), "JSON should be supported by flat implementation")
    assertTrue(dialects.contains(FLATBUFFERS), "Flatbuffers should be supported by flat implementation")
  }

  /** Ensure that the declared protocol implementation is `FLATBUFFERS`. */
  @Test override fun testExpectedLibrary() {
    assertEquals(FLAT, provide().engine(), "expected protocol implementation should be `FLATBUFFERS`")
  }
}
