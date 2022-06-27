package elide.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Abstract test base for [Encoder] objects. */
abstract class AbstractEncoderTest<V: Encoder> {
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
      assertNotNull(
        encoder.encode(someSample),
        "encoding directly to a byte array should not produce `null`"
      )
      assertNotNull(
        encoder.encodeToString(someSample),
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

  @Test fun testEncodeDecodeUUID() {
    for (i in 0 until 10) {
      val sample = UUID.random()
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
}
