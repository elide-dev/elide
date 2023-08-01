@file:Suppress(
  "JSUnresolvedFunction",
  "JSUnresolvedVariable",
  "JSVoidFunctionReturnValueUsed",
  "JSCheckFunctionSignatures",
)

package elide.runtime.gvm.internals.intrinsics.js.crypto

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.intrinsics.js.typed.UUIDValue
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.err.ValueError
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the [WebCryptoIntrinsic]. */
@TestCase internal class WebCryptoIntrinsicTest : AbstractJsIntrinsicTest<WebCryptoIntrinsic>() {
  // Console intrinsic under test.
  @Inject internal lateinit var crypto: WebCryptoIntrinsic

  override fun provide(): WebCryptoIntrinsic = crypto

  override fun testInjectable() {
    assertNotNull(crypto, "should be able to inject implementation of WebCrypto")
  }

  @Test fun testUUIDValueRandom() {
    val first = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    val second = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    assertNotEquals(first, second, "randomUUID should never return the same value")
    assertEquals(first, first, "a returned UUID should equal itself")
    assertNotNull(first.asString, "converting UUID value to string should not return `null`")
    assertEquals(first.asString, first.asString, "UUID toString should be able")
    assertNotEquals(first.asString, second.asString, "UUID toString should be unique for each random UUID")
    assertEquals(UUIDValue.UUID_LENGTH, first.length)
    assertEquals(UUIDValue.UUID_LENGTH, second.length)
    assertEquals(UUIDValue.UUID_LENGTH, first.asString.length)
    assertEquals(UUIDValue.UUID_LENGTH, second.asString.length)
  }

  @Test fun testUUIDValueCopy() {
    val first = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    val second = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    val same = assertNotNull(UUIDValue.of(first), "should be able to efficiently copy UUIDValue")
    assertEquals(first, first, "a returned UUID should equal itself")
    assertEquals(first, same, "a copied UUID should equal the original")
    assertNotEquals(first, second, "randomUUID should never return the same value")
    val firstString = first.asString
    assertEquals(firstString, same.asString, "UUID toString should be consistent within returned copies")
    assertEquals(first.asString, same.asString, "UUID toString should be stable within returned copies")
  }

  @Test fun testUUIDValueSubstring() {
    val first = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    val firstFiveChars = first.substring(0, 5)
    assertEquals(5, firstFiveChars.length, "substring should return the correct length")
    assertEquals(firstFiveChars, first.asString.substring(0, 5), "substring should return the correct value")
    val firstFiveChars2 = first.asString.substring(0, 5)
    assertEquals(5, firstFiveChars2.length, "substring should return the correct length")
    assertEquals(firstFiveChars2, first.asString.substring(0, 5), "substring should return the correct value")
  }

  @Test fun testUUIDValueGetChar() {
    val first = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    assertNotNull(first[0], "should be able to retrieve indexed character from UUID")
    assertEquals(first[0], first[0], "retrieving character by index should be stable")
    assertEquals(first[0], first.asString[0], "retrieving character by index should be identical to string")
    val test = "94F3C756-B7F8-4CED-9D69-45A170D74CD4"
    val uuid = assertNotNull(UUIDValue.of(test), "should be able to parse UUID from string")
    assertEquals('9', uuid[0], "retrieving character by index should be correct value")
  }

  @Test fun testUUIDValueRandomUnique() {
    // generate 1000 random UUIDs and check them for uniqueness
    val uuids = (0..1000).map { UUIDValue.random() }
    val unique = uuids.distinct()
    assertEquals(uuids.size, unique.size, "randomUUID should never return the same value")
  }

  @Test fun testUUIDValueLength() {
    val first = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    val second = assertNotNull(UUIDValue.random(), "randomUUID should never return `null`")
    assertEquals(UUIDValue.UUID_LENGTH, first.length)
    assertEquals(UUIDValue.UUID_LENGTH, second.length)
    assertEquals(UUIDValue.UUID_LENGTH, first.asString.length)
    assertEquals(UUIDValue.UUID_LENGTH, second.asString.length)
  }

  @Test fun testUUIDValueFromJava() {
    val rand = assertNotNull(UUID.randomUUID(), "randomUUID should never return `null`")
    val rand2 = assertNotNull(UUID.randomUUID(), "randomUUID should never return `null`")
    val wrapped = assertNotNull(UUIDValue.of(rand))
    val wrapped2 = assertNotNull(UUIDValue.of(rand))
    val wrappedOther = assertNotNull(UUIDValue.of(rand2))
    assertEquals(wrapped, wrapped, "wrapped Java UUID should equal itself")
    assertEquals(wrapped, wrapped2, "wrapped Java UUID should equal other instance if equivalent")
    assertNotEquals(wrapped, wrappedOther, "wrapped Java UUID should not equal other UUID")
  }

  @Test fun testUUIDValueFromString() {
    val rand = assertNotNull(UUID.randomUUID().toString(), "randomUUID should never return `null`")
    val rand2 = assertNotNull(UUID.randomUUID().toString(), "randomUUID should never return `null`")
    val parsed = assertNotNull(UUIDValue.of(rand))
    val parsed2 = assertNotNull(UUIDValue.of(rand))
    val parsedOther = assertNotNull(UUIDValue.of(rand2))
    assertEquals(parsed, parsed, "parsed string UUID should equal itself")
    assertEquals(parsed, parsed2, "parsed string UUID should equal other instance if equivalent")
    assertNotEquals(parsed, parsedOther, "parsed string UUID should not equal other UUID")
  }

  @Test fun testRandomUUID() = dual {
    val first = assertNotNull(crypto.randomUUID(), "randomUUID should never return `null`")
    val second = assertNotNull(crypto.randomUUID(), "randomUUID should never return `null`")
    assertNotEquals(first, second, "randomUUID should never return the same value")
    assertEquals(first, first, "a returned UUID should equal itself")
  }.thenRun {
    // language=javascript
    """
      const uuid = crypto.randomUUID();
      const uuid2 = crypto.randomUUID();
      console.log(uuid);
      console.log(uuid2);
      test(uuid).isNotNull("should not get `null` for `randomUUID`");
      test(uuid).isEqualTo(uuid);
      test(uuid).isNotEqualTo(uuid2);
      uuid;
    """
  }.thenAssert {
    assertNotNull(
      it.returnValue(),
      "should get a return value for first uuid from guest",
    )
  }

  @Test fun testUUIDParseError() {
    val test = "94F3C756-B7F8-4CED-9D69-45A170D74CD4"
    val uuid = assertNotNull(UUIDValue.of(test), "should be able to parse UUID from string")
    assertEquals('9', uuid[0], "retrieving character by index should be correct value")

    assertFailsWith<ValueError> {
      UUIDValue.of("not-a-uuid")
    }
  }

  @Test fun testRandomValuesHost() {
    val size = 64
    val empty = ByteArray(size)
    val data = ByteArray(size)
    crypto.getRandomValues(data)
    assertNotEquals(empty, data, "random bytes should not be empty")
    val data2 = ByteArray(size)
    crypto.getRandomValues(data2)
    assertNotEquals(data, data2, "random bytes should not be the same")
  }

  @Test fun testRandomValuesGuestInt8Array() = executeGuest {
    // language=javascript
    """
      const data = new Int8Array(64);
      crypto.getRandomValues(data);
      test(data).isNotNull("random bytes should not be empty");
      test(data).isEqualTo(data);  // sanity test
      test(data).isNotEqualTo(new Int8Array(64));
      test(data).isNotEqualTo(new Int8Array(64));
    """
  }.doesNotFail()

  @Test fun testRandomValuesGuestUInt8Array() = executeGuest {
    // language=javascript
    """
      const data = new Uint8Array(64);
      crypto.getRandomValues(data);
      test(data).isNotNull("random bytes should not be empty");
      test(data).isEqualTo(data);  // sanity test
      test(data).isNotEqualTo(new Uint8Array(64));
      test(data).isNotEqualTo(new Uint8Array(64));
    """
  }.doesNotFail()

  @Test fun testRandomValuesGuestInt16Array() = executeGuest {
    // language=javascript
    """
      const data = new Int16Array(64);
      crypto.getRandomValues(data);
      test(data).isNotNull("random bytes should not be empty");
      test(data).isEqualTo(data);  // sanity test
      test(data).isNotEqualTo(new Int16Array(64));
      test(data).isNotEqualTo(new Int16Array(64));
    """
  }.doesNotFail()

  @Test fun testRandomValuesGuestUInt32Array() = executeGuest {
    // language=javascript
    """
      const data = new Uint32Array(64);
      crypto.getRandomValues(data);
      test(data).isNotNull("random bytes should not be empty");
      test(data).isEqualTo(data);  // sanity test
      test(data).isNotEqualTo(new Uint32Array(64));
      test(data).isNotEqualTo(new Uint32Array(64));
    """
  }.doesNotFail()

  @Test fun testRandomValuesNonNumericArray() = executeGuest {
    // language=javascript
    """
      crypto.getRandomValues(["hi", "hello"]);
    """
  }.failsWith<ValueError>()

  @Test fun testRandomValuesNullArray() = executeGuest {
    // language=javascript
    """
      crypto.getRandomValues([null]);
    """
  }.failsWith<ValueError>()

  @Test fun testRandomValuesNonArray() = executeGuest {
    // language=javascript
    """
      crypto.getRandomValues({});
    """
  }.failsWith<ValueError>()

  @Test fun testRandomValuesAsGuestValue() = executeGuest {
    // language=javascript
    """
      new Int16Array(64);
    """
  }.thenAssert {
    val typedArray = it.returnValue()
    assertNotNull(
      typedArray,
      "should get numeric array (empty) as return value from guest",
    )
    crypto.getRandomValues(typedArray)
  }

  @Test fun testRandomValuesAsGuestValueInvalid() = executeGuest {
    // language=javascript
    """
      {}
    """
  }.thenAssert {
    val invalidObj = it.returnValue()
    assertNotNull(
      invalidObj,
      "should get numeric array (empty) as return value from guest",
    )
    assertFailsWith<ValueError> {
      crypto.getRandomValues(invalidObj)
    }
  }

  @Test fun testRandomValuesTooBigHost() {
    assertFailsWith<ValueError> {
      crypto.getRandomValues(ByteArray(65537))
    }
  }

  @Test fun testRandomValuesTooBigGuest() = executeGuest {
    // language=javascript
    """
      const data = new Uint8Array(65537);
      crypto.getRandomValues(data);
    """
  }.failsWith<ValueError>()

  @Test fun testSubtleCryptoAvailable() {
    assumeTrue {
      try {
        crypto.subtle
        true
      } catch (err: Throwable) {
        false
      }
    }
  }
}
