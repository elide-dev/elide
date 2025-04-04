package elide.http

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.*
import elide.core.api.Symbolic

class HttpStandardTest {
  @Test fun testUnresolved() {
    assertThrows<Symbolic.Unresolved> { StandardHeader.resolve("unknown-header") }
    assertThrows<Symbolic.Unresolved> { StandardMethod.resolve("UNKNOWN") }
    assertThrows<Symbolic.Unresolved> { StatusCode.StandardStatusCode.resolve(999u) }
  }

  @TestFactory fun testStandardHeader(): Stream<DynamicTest> = StandardHeader.all.map {
    DynamicTest.dynamicTest("StandardHeader(${it.symbol})") {
      val header = it.symbol
      assertNotNull(header)
      val resolved = StandardHeader.resolve(it.symbol)
      assertSame(it, resolved)
      assertEquals(it, resolved)
      assertNotNull(it.symbol)
      assertNotNull(it.name)
      assertNotNull(it.nameNormalized)
      assertNotNull(it.allowedOnResponses)
      assertNotNull(it.allowedOnRequests)
      val wrapped = HeaderName.StdHeaderName(it)
      assertNotNull(wrapped)
      assertEquals(it.symbol.lowercase(), wrapped.name.lowercase())
      assertEquals(it.allowedOnResponses, wrapped.allowedOnResponses)
      assertEquals(it.allowedOnRequests, wrapped.allowedOnRequests)
      assertEquals(it.nameNormalized, wrapped.nameNormalized)
      assertEquals(it.symbol, wrapped.name)
      val asCustom = HeaderName.StringHeaderName(it.symbol)
      assertNotNull(asCustom)
      assertEquals(it.symbol.lowercase(), asCustom.name.lowercase())
      assertTrue(asCustom.allowedOnResponses)
      assertTrue(asCustom.allowedOnRequests)
      assertNotNull(it.toString())
      assertNotNull(it.asString())
      assertTrue(it.toString().isNotEmpty())
      assertTrue(it.toString().isNotBlank())
      assertTrue(it.asString().isNotEmpty())
      assertTrue(it.asString().isNotBlank())
    }
  }.asStream()

  @TestFactory fun testStandardMethod(): Stream<DynamicTest> = StandardMethod.all.map {
    DynamicTest.dynamicTest("StandardMethod(${it.symbol})") {
      val header = it.symbol
      assertNotNull(header)
      val resolved = StandardMethod.resolve(it.symbol)
      assertSame(it, resolved)
      assertEquals(it, resolved)
      assertNotNull(it.symbol)
      assertNotNull(it.name)
      assertNotNull(it.permitsRequestBody)
      assertNotNull(it.permitsResponseBody)
      assertNotNull(it.requiresRequestBody)
      assertNotNull(it.toString())
      assertNotNull(it.asString())
      assertTrue(it.toString().isNotEmpty())
      assertTrue(it.toString().isNotBlank())
      assertTrue(it.asString().isNotEmpty())
      assertTrue(it.asString().isNotBlank())
    }
  }.asStream()

  @TestFactory fun testStandardStatus(): Stream<DynamicTest> = StatusCode.StandardStatusCode.all.map {
    DynamicTest.dynamicTest("StandardStatus(${it.symbol})") {
      val header = it.symbol
      assertNotNull(header)
      val resolved = StatusCode.StandardStatusCode.resolve(it.symbol)
      assertSame(it, resolved)
      assertEquals(it, resolved)
      assertNotNull(it.symbol)
      assertNotNull(it.name)
      assertNotNull(it.reasonPhrase)
      assertNotNull(it.toString())
      assertNotNull(it.asString())
      assertTrue(it.toString().isNotEmpty())
      assertTrue(it.toString().isNotBlank())
      assertTrue(it.asString().isNotEmpty())
      assertTrue(it.asString().isNotBlank())
    }
  }.asStream()

  @Test fun testCustomStatusCodes() {
    val custom = StatusCode.CustomStatusCode(999u)
    val customResolved = StatusCode.CustomStatusCode.resolve(999u)
    assertNotNull(custom)
    assertNotNull(customResolved)
    assertThrows<Symbolic.Unresolved> {
      StatusCode.CustomStatusCode.resolve(1000u)
    }
    assertEquals(999u, custom.symbol)
    assertEquals("999", custom.asString())
    assertEquals("999", custom.toString())
    val isCustom = StatusCode.resolve(999u)
    assertNotNull(isCustom)
    assertTrue(isCustom is StatusCode.CustomStatusCode)
    assertEquals(custom, isCustom)
    assertEquals(custom.symbol, isCustom.symbol)
    assertEquals(custom.asString(), isCustom.asString())
    assertEquals(custom.toString(), isCustom.toString())
    val isStd = StatusCode.resolve(200u)
    assertNotNull(isStd)
    assertTrue(isStd is StatusCode.StandardStatusCode)
    val customWithReason = StatusCode.CustomStatusCode(600u, "Testing")
    assertNotNull(customWithReason)
    assertEquals(600u, customWithReason.symbol)
    assertEquals("600 Testing", customWithReason.asString())
    val customWithoutReason = StatusCode.CustomStatusCode(600u)
    assertNotNull(customWithoutReason)
    assertEquals(600u, customWithoutReason.symbol)
    assertEquals("600", customWithoutReason.asString())
  }

  @Test fun testMessageType() {
    Message.Type.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertNotNull(it.toString())
    }
  }

  @Test fun testProtocolVersion() {
    ProtocolVersion.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.toString())
      assertNotNull(it.major)
      assertNotNull(it.minor)
      assertNotNull(it.symbol)
      assertNotNull(ProtocolVersion.resolve(it.symbol))
    }
  }

  @Test fun testProtocolVersionHttp1_0() {
    val vn = ProtocolVersion.HTTP_1_0
    assertEquals(vn, ProtocolVersion.resolve("HTTP/1.0"))
    assertEquals(1u, vn.major)
    assertEquals(0u, vn.minor)
    assertNotNull(vn.toString())
    assertTrue(vn.toString().isNotEmpty())
    assertTrue(vn.toString().isNotBlank())
    assertNotNull(vn.asString())
    assertTrue(vn.asString().isNotBlank())
    assertTrue(vn.asString().isNotEmpty())
    assertFalse(vn.supportsPipelining())
    assertFalse(vn.supportsChunkedTransferEncoding())
    assertFalse(vn.supportsTrailers())
    assertFalse(vn.supportsPush())
  }

  @Test fun testProtocolVersionHttp1_1() {
    val vn = ProtocolVersion.HTTP_1_1
    assertEquals(vn, ProtocolVersion.resolve("HTTP/1.1"))
    assertEquals(1u, vn.major)
    assertEquals(1u, vn.minor)
    assertTrue(vn.supportsPipelining())
    assertTrue(vn.supportsChunkedTransferEncoding())
    assertFalse(vn.supportsTrailers())
    assertFalse(vn.supportsPush())
  }

  @Test fun testProtocolVersionHttp2_0() {
    val vn = ProtocolVersion.HTTP_2
    assertEquals(vn, ProtocolVersion.resolve("HTTP/2"))
    assertEquals(2u, vn.major)
    assertEquals(0u, vn.minor)
    assertFalse(vn.supportsPipelining())
    assertTrue(vn.supportsChunkedTransferEncoding())
    assertTrue(vn.supportsTrailers())
    assertTrue(vn.supportsPush())
  }

  @Test fun testProtocolVersionUnresolved() {
    assertThrows<Symbolic.Unresolved> { ProtocolVersion.resolve("HTTP/0.9") }
  }

  @Test fun testCustomMethod() {
    val custom = Method.CustomMethod("EXAMPLE")
    assertNotNull(custom)
    assertEquals("EXAMPLE", custom.symbol)
    assertTrue(custom.permitsRequestBody)
    assertTrue(custom.permitsResponseBody)
    assertFalse(custom.requiresRequestBody)
    val other = Method.of("OTHER")
    val example2 = Method.of("EXAMPLE")
    assertEquals(custom.symbol, example2.symbol)
    assertEquals("OTHER", other.symbol)
    assertNotSame(custom, other)
    assertNotSame(custom, example2)
    assertNotNull(Method.Companion)
    assertNotNull(Method.toString())
  }
}
