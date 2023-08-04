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
import elide.proto.ElideProtocol.Dialect.JSON
import elide.proto.ElideProtocol.Dialect.PROTO
import elide.proto.ElideProtocol.ImplementationLibrary.PROTOBUF
import elide.proto.api.Protocol
import elide.proto.test.AbstractProtocolFacadeTest
import elide.testing.annotations.Test

/** Tests to load the Elide Protocol implementation backed by Protocol Buffers. */
internal class ProtobufProtocolTest : AbstractProtocolFacadeTest<ElideProtoJava>() {
  /** @inheritDoc */
  override fun provide(): ElideProtoJava = Protocol.acquire(PROTOBUF) as ElideProtoJava

  /** Test expected dialects for protocol buffers implementation. */
  @Test override fun testExpectedDialects() {
    val dialects = provide().dialects()
    assertTrue(dialects.contains(JSON), "JSON should be supported by protobuf implementation")
    assertTrue(dialects.contains(PROTO), "proto-binary should be supported by protobuf implementation")
  }

  /** Ensure that the declared protocol implementation is `PROTOBUF`. */
  @Test override fun testExpectedLibrary() {
    assertEquals(PROTOBUF, provide().engine(), "expected protocol implementation should be `PROTOBUF`")
  }
}
