package elide.runtime.gvm.internals.node.buffer

import java.nio.charset.Charset

/**
 * A collection of helper functions handling encoding and decoding of strings with supported encodings in Node.js APIs.
 */
internal object NodeEncodingHelper {
  /**
   * Use an [encoding] string to determine the correct encoding (defaulting to UTF-8) to use for an input [string].
   * Returns the encoded bytes.
   */
  internal fun encode(string: String, encoding: String?): ByteArray {
    val charset = encoding?.let { Charset.forName(it) } ?: Charsets.UTF_8
    return string.toByteArray(charset)
  }

  /**
   * Use an [encoding] string to determine the correct encoding (defaulting to UTF-8) to use for the input [bytes].
   * Returns the decoded string.
   */
  internal fun decode(bytes: ByteArray, encoding: String?): String {
    val charset = encoding?.let { Charset.forName(it) } ?: Charsets.UTF_8
    return bytes.toString(charset)
  }
}
