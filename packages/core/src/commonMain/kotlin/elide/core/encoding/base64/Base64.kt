package elide.core.encoding.base64

import elide.core.annotations.Static
import elide.core.encoding.*
import elide.core.encoding.Encoding

/**
 * # Base64
 *
 * This class consists exclusively of static methods, optimized for each platform, for obtaining encoders and decoders
 * for the Base64 encoding scheme. This implementation supports Base64 encodings as specified in
 * [RFC 4648](http://www.ietf.org/rfc/rfc4648.txt) and [RFC 2045](http://www.ietf.org/rfc/rfc2045.txt).
 *
 * This object defines the layout expected for each platform-specific implementation. Please see documentation for each
 * implementation to understand how each fulfill this interface.
 *
 * ## Basic Encoding
 *
 * Uses "The Base64 Alphabet" as specified in Table 1 of RFC 4648 and RFC 2045 for encoding and decoding operation. The
 * encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters
 * outside the base64 alphabet.
 *
 * ## URL-safe Encoding
 *
 * Uses the "URL and Filename safe Base64 Alphabet" as specified in Table 2 of RFC 4648 for encoding and decoding. The
 * encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters
 * outside the base64 alphabet.
 *
 * ## MIME
 *
 * Uses "The Base64 Alphabet" as specified in Table 1 of RFC 2045 for encoding and decoding operation. The encoded
 * output must be represented in lines of no more than 76 characters each and uses a carriage return `'\r'` followed
 * immediately by a linefeed `'\n'` as the line separator. No line separator is added to the end of the encoded output.
 *
 * All line separators or other characters not found in the base64 alphabet table are ignored in the decoding operation.
 *
 * Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to be
 * thrown.
 *
 * @author Sam Gammon
 */
public expect object Base64 : Codec<Base64Data> {
  /**
   * This class implements an encoder for encoding byte data using the Base64 encoding scheme as specified in RFC 4648
   * and RFC 2045.
   *
   * Instances of [Encoder] class are safe for use by multiple concurrent threads.
   *
   * Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to
   * be thrown.
   *
   * @see Decoder
   */
  public class Encoder() {
    public companion object {
      /**
       * Default encoder instance.
       */
      @Static
      public val DEFAULT: Encoder

      /**
       * Default encoder instance for un-padded encoding.
       */
      @Static
      public val DEFAULT_WEBSAFE: Encoder
    }

    /**
     * Encodes all bytes from the specified byte array into a newly-allocated byte array using the [Base64] encoding
     * scheme.The returned byte array is of the length of the resulting bytes.
     *
     * @param src the byte array to encode
     * @return A newly-allocated byte array containing the resulting encoded bytes.
     */
    public fun encode(src: ByteArray): ByteArray
  }

  /**
   * This class implements a decoder for decoding byte data using the Base64 encoding scheme as specified in RFC 4648
   * and RFC 2045.
   *
   * The Base64 padding character `'='` is accepted and interpreted as the end of the encoded byte data, but is not
   * required. So if the final unit of the encoded byte data only has two or three Base64 characters (without the
   * corresponding padding character(s) padded), they are decoded as if followed by padding character(s). If there is a
   * padding character present in the final unit, the correct number of padding character(s) must be present, otherwise
   * `IllegalArgumentException` (`IOException` when reading from a Base64 stream) is thrown during decoding.
   *
   * Instances of [Decoder] class are safe for use by multiple concurrent threads.
   *
   * Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to
   * be thrown.
   *
   * @see Encoder
   */
  public class Decoder() {
    public companion object {
      /** Default decoder instance. */
      @Static
      public val DEFAULT: Decoder
    }

    /**
     * Decodes all bytes from the input byte array using the [Base64] encoding scheme, writing the results into a
     * newly-allocated output byte array. The returned byte array is of the length of the resulting bytes.
     *
     * @param src the byte array to decode
     * @return A newly-allocated byte array containing the decoded bytes.
     * @throws IllegalArgumentException if `src` is not in valid Base64 scheme
     */
    public fun decode(src: ByteArray): ByteArray
  }

  /** @inheritDoc */
  override fun encoding(): Encoding

  /** @inheritDoc */
  override fun encoder(): elide.core.encoding.Encoder<Base64Data>

  /** @inheritDoc */
  override fun decode(data: Base64Data): ByteArray

  /** @inheritDoc */
  override fun decoder(): elide.core.encoding.Decoder<Base64Data>

  /**
   * Encode the provided [string] into a Base64-encoded string, omitting characters which are unsafe for use on the web,
   * including padding characters, which are not emitted.
   *
   * @param string String to encode with web-safe Base64.
   * @return Base64-encoded string, using only web-safe characters.
   */
  public fun encodeWebSafe(string: String): String

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the
   * web, including padding characters, which are not emitted.
   *
   * @param data Raw bytes to encode with web-safe Base64.
   * @return Base64-encoded bytes, using only web-safe characters.
   */
  public fun encodeWebSafe(data: ByteArray): ByteArray
}
