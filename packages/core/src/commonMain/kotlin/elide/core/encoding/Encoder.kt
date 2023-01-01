package elide.core.encoding

/**
 * # Encoder
 */
public interface Encoder<Raw : EncodedData> : CodecIdentifiable {
  // -- Methods: Encoding -- //
  /**
   * Encoded the provided [data] using the [encoding] implemented by this encoder; the returned [Raw] instance is able
   * to provide a resulting [ByteArray] or [String].
   *
   * @param data Data to encode with this encoder.
   * @return Raw encoded data record.
   */
  public fun encode(data: ByteArray): Raw

  /**
   * Encode the provided [data] using the [encoding] implemented by this encoder; return a [ByteArray] representation
   * of the result.
   *
   * @param data Data to encode with this encoder.
   * @return Raw encoded output data.
   */
  public fun encodeBytes(data: ByteArray): ByteArray = encode(data).data

  /**
   * Encode the provided [string] data using the [encoding] implemented by this encoder; by default, the string will
   * be interpreted using `UTF-8` encoding, then encoded to the target encoding.
   *
   * @param string String to encode with this encoder.
   * @return Encoded bytes of the provided string.
   */
  public fun encodeString(string: String): ByteArray = encode(string.encodeToByteArray()).data

  /**
   * Encode the provided [data] to a string representation using the [encoding] implemented by this encoder.
   *
   * @param data Raw data to encode in the target encoding and return as a string.
   * @return String representation of the encoded data.
   */
  public fun encodeToString(data: ByteArray): String = encode(data).string

  /**
   * Encode the provided [string] to a string representation using the [encoding] implemented by this encoder; the
   * string is interpreted using `UTF-8` encoding.
   *
   * @param string String to encode in the target encoding and return as a string.
   * @return String representation of the encoded data.
   */
  public fun encodeToString(string: String): String = encode(string.encodeToByteArray()).string
}
