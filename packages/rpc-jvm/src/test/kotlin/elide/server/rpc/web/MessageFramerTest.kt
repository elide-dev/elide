package elide.server.rpc.web

import kotlin.test.Test
import kotlin.test.assertEquals


/** Tests for [MessageFramer]. */
class MessageFramerTest {
  @Test fun testProcessSingleFrame() {
    val source = "source input stream"
    val bytes = source.toByteArray()
    val prefix = MessageFramer.getPrefix(bytes, RpcSymbol.DATA)
    assertEquals(5, prefix.size)
  }
}
