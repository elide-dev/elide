package elide.core.encoding

/**
 * # Codec
 *
 * Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or
 * expression, as well as decoding from that same format.
 *
 * @param Raw Value class which wraps values produced by this encoder.
 * @see EncodedData for the interface each encoded-data record complies with.
 * @see Decoder for the interface description for decoding.
 * @see Encoding for the interface description for encoding.
 */
public interface Codec<Raw : EncodedData> : Encoder<Raw>, Decoder<Raw>, CodecIdentifiable {
  /**
   * ## Decoder
   *
   * Provide a [Decoder] specialized to the format implemented by this [Codec]; the resulting object can only guarantee
   * a capability to decode data from the subject encoding.
   */
  public fun encoder(): Encoder<Raw> {
    return this
  }

  /**
   * ## Encoder
   *
   * Provide an [Encoder] specialized to the format implemented by this [Codec]; the resulting object can only guarantee
   * a capability to encode data to the target encoding.
   */
  public fun decoder(): Decoder<Raw> {
    return this
  }
}
