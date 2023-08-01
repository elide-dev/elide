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
