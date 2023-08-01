@file:Suppress("JSUnresolvedVariable")

package elide.runtime.gvm.internals.intrinsics.js.base64

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for intrinsic JS Base64 implementation. */
@TestCase internal class JsBase64IntrinsicTest : AbstractJsIntrinsicTest<Base64Intrinsic>() {
  // Console intrinsic under test.
  @Inject internal lateinit var base64: Base64Intrinsic

  override fun provide(): Base64Intrinsic = base64

  @Test override fun testInjectable() {
    assertNotNull(base64, "should be able to resolve base64 intrinsic via injection")
  }

  @Test fun testEncode() {
    val encoded = base64.encode("Hello, world!")
    assertNotNull(encoded, "should be able to encode string")
    assertTrue(encoded.isNotEmpty(), "should be able to encode string")
    assertEquals("SGVsbG8sIHdvcmxkIQ==", encoded, "should be able to encode string")
  }

  @Test fun testDecode() {
    val decoded = base64.decode("SGVsbG8sIHdvcmxkIQ==")
    assertNotNull(decoded, "should be able to decode string")
    assertTrue(decoded.isNotEmpty(), "should be able to decode string")
    assertEquals("Hello, world!", decoded, "should be able to decode string")
  }

  @Test fun testEncodeDecode() {
    val encoded = base64.encode("Hello, world!")
    assertNotNull(encoded, "should be able to encode string")
    assertTrue(encoded.isNotEmpty(), "should be able to encode string")
    assertEquals("SGVsbG8sIHdvcmxkIQ==", encoded, "should be able to encode string")
    val decoded = base64.decode(encoded)
    assertNotNull(decoded, "should be able to decode string")
    assertTrue(decoded.isNotEmpty(), "should be able to decode string")
    assertEquals("Hello, world!", decoded, "should be able to decode string")
  }

  @Test fun testGuestEncode() = executeGuest {
    // language=javascript
    """
      Base64.encode("Hello, world!");
    """
  }.thenAssert {
    assertNotNull(
      it.returnValue(),
      "should get a return value for guest base64 encode",
    )
    assertEquals(
      "SGVsbG8sIHdvcmxkIQ==",
      it.returnValue()?.asString(),
      "should get a return value for guest base64 encode"
    )
  }

  @Test fun testGuestDecode() = executeGuest {
    // language=javascript
    """
      Base64.decode("SGVsbG8sIHdvcmxkIQ==");
    """
  }.thenAssert {
    assertNotNull(
      it.returnValue(),
      "should get a return value for guest base64 decode",
    )
    assertEquals(
      "Hello, world!",
      it.returnValue()?.asString(),
      "should get a return value for guest base64 decode"
    )
  }

  @Test fun testGuestEncodeStd() = executeGuest {
    // language=javascript
    """
      btoa("Hello, world!");
    """
  }.thenAssert {
    assertNotNull(
      it.returnValue(),
      "should get a return value for guest base64 encode (`btoa`)",
    )
    assertEquals(
      "SGVsbG8sIHdvcmxkIQ==",
      it.returnValue()?.asString(),
      "should get a return value for guest base64 encode (`btoa`)"
    )
  }

  @Test fun testGuestDecodeStd() = executeGuest {
    // language=javascript
    """
      atob("SGVsbG8sIHdvcmxkIQ==");
    """
  }.thenAssert {
    assertNotNull(
      it.returnValue(),
      "should get a return value for guest base64 decode (`atob`)",
    )
    assertEquals(
      "Hello, world!",
      it.returnValue()?.asString(),
      "should get a return value for guest base64 decode (`atob`)"
    )
  }

  @CsvSource(value = [
    "Hello world!,SGVsbG8gd29ybGQh,SGVsbG8gd29ybGQh",
    "12346789,MTIzNDY3ODk=,MTIzNDY3ODk",
    "f,Zg==,Zg",
    "fo,Zm8=,Zm8",
    "foo,Zm9v,Zm9v",
    "foob,Zm9vYg==,Zm9vYg",
    "fooba,Zm9vYmE=,Zm9vYmE",
    "foobar,Zm9vYmFy,Zm9vYmFy",
  ])
  @ParameterizedTest(name = "[{index}]") fun testCodec(source: String, expected: String, websafeExpected: String) {
    // first, encode and decode directly
    val encoded = base64.encode(source)
    assertNotNull(encoded, "should be able to encode string as base64")
    assertTrue(encoded.isNotEmpty(), "should be able to encode string as base64")
    assertEquals(expected, encoded, "should get expected base64-encoded result")

    // check stability
    val encoded2 = base64.encode(source)
    assertNotNull(encoded2)
    assertTrue(encoded2.isNotEmpty())
    assertEquals(encoded, encoded2, "should get stable base64-encoded result")

    // check decoded
    val decoded = base64.decode(encoded)
    assertNotNull(decoded)
    assertTrue(decoded.isNotEmpty())
    assertEquals(source, decoded, "should get decoded string after decoding base64 string")

    // check decoding stability
    val decoded2 = base64.decode(encoded)
    assertNotNull(decoded2)
    assertTrue(decoded2.isNotEmpty())
    assertEquals(decoded, decoded2, "should get stable decoded string after decoding base64 string")

    // check round-trip
    val decoded3 = base64.decode(base64.encode(source))
    assertNotNull(decoded3)
    assertTrue(decoded3.isNotEmpty())
    assertEquals(source, decoded3, "should get decoded string after round-trip encoding/decoding")

    val websafe = base64.encode(source, true)
    assertNotNull(websafe, "should be able to encode a string in websafe base64")
    assertTrue(websafe.isNotEmpty(), "should be able to encode a string in websafe base64")
    assertEquals(websafeExpected, websafe, "should get expected websafe base64-encoded result")
    val decoded4 = base64.decode(websafe)
    assertNotNull(decoded4, "should not get `null` from decoding websafe base64")
    assertTrue(decoded4.isNotEmpty(), "should not get empty string from decoding websafe base64")
    assertEquals(source, decoded4, "should get decoded string after decoding websafe base64 string")

    // check guest encode
    executeGuest {
      // language=javascript
      """
        Base64.encode("$source");
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 encode",
      )
      assertEquals(
        encoded,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 encode"
      )
    }

    // check guest encode web-safe
    executeGuest {
      // language=javascript
      """
        Base64.encode("$source", true);
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 encode",
      )
      assertEquals(
        websafeExpected,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 encode"
      )
    }

    // check guest decode
    executeGuest {
      // language=javascript
      """
        Base64.decode("$encoded");
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 decode",
      )
      assertEquals(
        source,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 decode"
      )
    }

    // check guest decode web-safe
    executeGuest {
      // language=javascript
      """
        Base64.decode("$websafe");
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 decode (websafe)",
      )
      assertEquals(
        source,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 decode (websafe)"
      )
    }

    // check guest round trip
    executeGuest {
      // language=javascript
      """
        Base64.decode(Base64.encode("$source"));
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 round-trip",
      )
      assertEquals(
        source,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 round-trip"
      )
    }

    // check guest round trip standard
    executeGuest {
      // language=javascript
      """
        atob(btoa("$source"));
      """
    }.thenAssert {
      assertNotNull(
        it.returnValue(),
        "should get a return value for guest base64 round-trip (`atob`/`btoa`)",
      )
      assertEquals(
        source,
        it.returnValue()?.asString(),
        "should get a return value for guest base64 round-trip (`atob`/`btoa`)"
      )
    }
  }
}
