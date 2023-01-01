package elide.core.encoding

/**
 * # Encoded Data
 *
 * Specifies the interface which encoded data value classes comply with; each encoded data class indicates its encoding,
 * and provides a means for decoding to/from strings.
 *
 * @param T Underlying data type. Either a [ByteArray] or [String].
 */
public interface EncodedData {
  /** Indicate the encoding type applied to the data held by this object. */
  public val encoding: Encoding

  /** Raw data associated with this encoded data payload. */
  public val data: ByteArray

  /** Decode the underlying data to a string. */
  public val string: String
}
