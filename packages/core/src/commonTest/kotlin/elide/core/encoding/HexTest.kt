package elide.core.encoding

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for built-in [Hex] encoding tools. */
class HexTest : AbstractEncoderTest<Hex>() {
  override fun encoding(): Encoding = Encoding.HEX
  override fun encoder(): Hex = Hex

  @Test fun testEncodeHexFromByteArray() {
    val value = "abc123123"
    val hex = Hex.bytesToHex(value.encodeToByteArray())
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromString() {
    val value = "abc123123"
    val hex = Hex.encodeToString(value)
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromByteArrayExtension() {
    val value = "abc123123".encodeToByteArray()
    val hex = value.toHexString()
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromStringExtension() {
    val value = "abc123123"
    val hex = value.toHexString()
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
    val hex2 = value.toHex().string
    assertEquals(
      "616263313233313233",
      hex2,
      "hex encoding a basic value should produce expected result"
    )
  }
}
