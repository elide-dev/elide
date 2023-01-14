package elide.core.encoding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Abstract test base for [Encoder] objects. */
abstract class AbstractEncoderTest<V: Codec<*>> {
  /**
   * @return Encoding that is tested under this class.
   */
  abstract fun encoding(): Encoding

  /**
   * @return Encoder object/instance under testing.
   */
  abstract fun encoder(): V

  @Test fun testBasicEncodingStability() {
    val someSample = "jknhbgvtyiuhjknbhvgfvytguihojlknm"
    val encoder = encoder()
    val encoded = encoder.encodeToString(someSample)
    assertNotNull(
      encoder,
      "should be able to acquire encoder instance"
    )
    assertNotNull(
      encoded,
      "should not get `null` as encoded result"
    )
    for (i in 0 until 10) {
      val encoded2 = encoder.encodeToString(someSample)
      assertEquals(
        encoded,
        encoded2,
        "encoding produce deterministic results"
      )
      val bytes: ByteArray = encoder.encodeString(someSample)
      assertNotNull(
        bytes,
        "encoding directly to a byte array should not produce `null`"
      )
      val string: String = encoder.encodeToString(someSample)
      assertNotNull(
        string,
        "should not get `null` when encoding to a string"
      )
      assertNotNull(
        encoder.encode(someSample.encodeToByteArray()),
        "should not get `null` when encoding from a byte array"
      )
      assertNotNull(
        encoder.encodeToString(someSample.encodeToByteArray()),
        "should not get `null` when encoding from a byte array"
      )
    }
  }

  @Test fun testEncodeDecode() {
    val subjects = listOf(
      "kljbhgyuiojklmnjbhgvyhuioj;klmnbhgjvygouioj;klm",
      "here is a sample bla bla",
      "another sample set of data",
      "abcdefghijklmnopqrstuvwxyz123456789!@#$%^&*()-_=+[{]}|,<.>/?;:'",
    )
    subjects.forEach { sample ->
      val encoded = encoder().encodeToString(
        sample
      )
      assertNotNull(
        encoded,
        "should get non-null output from encoder (${encoding().name})"
      )
      val decoded = encoder().decodeToString(
        encoded
      )
      assertEquals(
        sample,
        decoded,
        "sample should decode properly from encoded string (${encoding().name})"
      )
    }
  }

  @Test fun testEncodeDecodeRandomData() {
    for (i in 0 until 10) {
      val sample = "0EFFA6ED-01C9-4AB1-9C80-1BB986046D84"
      val encoded = encoder().encodeToString(
        sample
      )
      assertNotNull(
        encoded,
        "should get non-null output from encoder (${encoding().name})"
      )
      val decoded: String = encoder().decodeToString(encoded)
      assertEquals(
        sample,
        decoded,
        "sample should decode properly from encoded string (${encoding().name})"
      )
    }
  }
}
