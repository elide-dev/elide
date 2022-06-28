package elide.util

/**
 * Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or
 * expression, as well as decoding from that same format.
 */
public interface Encoder {
  /**
   * Return the enumerated [Encoding] which is implemented by this [Encoder].
   *
   * @return Enumerated encoding type.
   */
  public fun encoding(): Encoding

  // -- Methods: Encoding -- //
  /**
   *
   */
  public fun encode(data: ByteArray): ByteArray

  /**
   *
   */
  public fun encode(string: String): ByteArray

  /**
   *
   */
  public fun encodeToString(data: ByteArray): String

  /**
   *
   */
  public fun encodeToString(string: String): String

  // -- Methods: Decoding -- //
  /**
   *
   */
  public fun decode(data: ByteArray): ByteArray

  /**
   *
   */
  public fun decode(string: String): ByteArray

  /**
   *
   */
  public fun decodeToString(data: ByteArray): String

  /**
   *
   */
  public fun decodeToString(string: String): String
}
