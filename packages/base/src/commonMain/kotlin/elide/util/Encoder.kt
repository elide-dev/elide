package elide.util

/**
 * # Encoder
 *
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
   * ## Encode: Bytes to Bytes
   *
   * Encode the provided [data], returning the raw resulting bytes.
   *
   * @param data Data to encode.
   * @return Encoded data.
   */
  public fun encode(data: ByteArray): ByteArray

  /**
   * ## Encode: String to Bytes
   *
   * Encode the provided [string], returning the raw resulting bytes.
   *
   * @param string String to encode.
   * @return Encoded data.
   */
  public fun encode(string: String): ByteArray

  /**
   * ## Encode: Bytes to String
   *
   * Encode the provided [data], returning an encoded string.
   *
   * @param data Bytes to encode.
   * @return Encoded string.
   */
  public fun encodeToString(data: ByteArray): String

  /**
   * ## Encode: String to String
   *
   * Encode the provided [string], returning an encoded string.
   *
   * @param string String to encode.
   * @return Encoded string.
   */
  public fun encodeToString(string: String): String

  // -- Methods: Decoding -- //
  /**
   * ## Decode: Bytes to Bytes
   *
   * Decode the provided [data], returning the raw resulting bytes.
   *
   * @param data Data to decode.
   * @return Decoded data.
   */
  public fun decode(data: ByteArray): ByteArray

  /**
   * ## Decode: String to Bytes
   *
   * Decode the provided [string], returning the raw resulting bytes.
   *
   * @param string String to decode.
   * @return Decoded data.
   */
  public fun decode(string: String): ByteArray

  /**
   * ## Decode: Bytes to String
   *
   * Decode the provided [data], returning a decoded string.
   *
   * @param data Data to decode.
   * @return Decoded string.
   */
  public fun decodeToString(data: ByteArray): String

  /**
   * ## Decode: String to String
   *
   * Decode the provided [string], returning a decoded string.
   *
   * @param string String to decode.
   * @return Decoded string.
   */
  public fun decodeToString(string: String): String
}
