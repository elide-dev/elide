package elide.proto.impl

import elide.proto.ElideProtocol.Dialect.*
import elide.proto.ElideProtocol.ImplementationLibrary.FLATBUFFERS as FLAT
import elide.proto.api.Protocol
import elide.proto.test.AbstractProtocolFacadeTest
import elide.testing.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
