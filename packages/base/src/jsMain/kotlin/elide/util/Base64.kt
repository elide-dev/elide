package elide.util

import kotlinx.browser.window

/** Cross-platform utilities for encoding and decoding to/from Base64. */
actual object Base64 {
  /**
   * Encode the provided [string] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param string String to encode with Base64.
   * @return Base64-encoded string.
   */
  actual fun encode(string: String): String {
    return window.btoa(string)
  }

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, which includes padding if necessary.
   *
   * @param data Raw bytes to encode with Base64.
   * @return Base64-encoded bytes.
   */
  actual fun encode(data: ByteArray): ByteArray {
    return window.btoa(data.decodeToString()).encodeToByteArray()
  }

  /**
   * Encode the provided [string] into a Base64-encoded string, omitting characters which are unsafe for use on the web,
   * including padding characters, which are not emitted.
   *
   * @param string String to encode with web-safe Base64.
   * @return Base64-encoded string, using only web-safe characters.
   */
  actual fun encodeWebSafe(string: String): String {
    return this.encode(string).replace("=", "")
  }

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the
   * web, including padding characters, which are not emitted.
   *
   * @param data Raw bytes to encode with web-safe Base64.
   * @return Base64-encoded bytes, using only web-safe characters.
   */
  actual fun encodeWebSafe(data: ByteArray): ByteArray {
    return this.encode(data).decodeToString().replace("=", "").encodeToByteArray()
  }
}
