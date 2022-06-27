package elide.util

/**
 * Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or
 * expression, as well as decoding from that same format.
 */
interface Encoder {
  /**
   * Return the enumerated [Encoding] which is implemented by this [Encoder].
   *
   * @return Enumerated encoding type.
   */
  fun encoding(): Encoding

  // -- Methods: Encoding -- //
  /**
   *
   */
  fun encode(data: ByteArray): ByteArray

  /**
   *
   */
  fun encode(string: String): ByteArray

  /**
   *
   */
  fun encodeToString(data: ByteArray): String

  /**
   *
   */
  fun encodeToString(string: String): String

  // -- Methods: Decoding -- //
  /**
   *
   */
  fun decode(data: ByteArray): ByteArray

  /**
   *
   */
  fun decode(string: String): ByteArray

  /**
   *
   */
  fun decodeToString(data: ByteArray): String

  /**
   *
   */
  fun decodeToString(string: String): String
}
