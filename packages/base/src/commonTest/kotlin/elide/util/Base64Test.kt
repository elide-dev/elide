package elide.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for built in [Base64] tools. */
class Base64Test: AbstractEncoderTest<Base64>() {
  override fun encoding(): Encoding = Encoding.BASE64
  override fun encoder(): Base64 = Base64

  @Test fun testEncodeBase64() {
    val input = "abc123123"
    assertEquals(
      "YWJjMTIzMTIz",
      Base64.encodeToString(input),
      "should get expected output from Base64 encode"
    )
    val inputPadded = "abc1231231"
    assertEquals(
      "YWJjMTIzMTIzMQ==",
      Base64.encodeToString(inputPadded),
      "should get expected output from non-websafe Base64 encode"
    )
    assertEquals(
      "YWJjMTIzMTIzMQ==",
      Base64.encodeWebSafe(inputPadded),
      "should get expected output from websafe Base64 encode"
    )
  }

  @Test fun testEncodeBase64Extension() {
    val input = "abc123123"
    assertEquals(
      "YWJjMTIzMTIz",
      input.toBase64String(),
      "should get expected output from Base64 encode"
    )
  }

  @Test fun testDecodeBase64() {
    val input = "YWJjMTIzMTIz"
    assertEquals(
      "abc123123",
      Base64.decodeToString(input),
      "should get expected output from Base64 decode"
    )
  }

  @Test fun testEncodeDecodeBase64() {
    val subjects = listOf(
      "kljbhgyuiojklmnjbhgvyhuioj;klmnbhgjvygouioj;klm",
      "here is a sample bla bla",
    )
    subjects.forEach { sample ->
      val encoded = Base64.encodeToString(
        sample
      )
      assertNotNull(
        encoded,
        "should get non-null output from base64 encoder"
      )
      val decoded = Base64.decodeToString(
        encoded
      )
      assertEquals(
        sample,
        decoded,
        "sample should decode properly from encoded base64 string"
      )
    }
  }
}
