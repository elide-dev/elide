package elide.core.encoding

import elide.core.encoding.hex.DefaultHex
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests for built-in [DefaultHex] encoding tools. */
class HexTest : AbstractEncoderTest<DefaultHex>() {
  override fun encoding(): Encoding = Encoding.HEX
  override fun encoder(): DefaultHex = DefaultHex

  @Test fun testEncodeHexFromByteArray() {
    val value = "abc123123"
    val hex = DefaultHex.bytesToHex(value.encodeToByteArray())
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromString() {
    val value = "abc123123"
    val hex = DefaultHex.encodeToString(value)
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
