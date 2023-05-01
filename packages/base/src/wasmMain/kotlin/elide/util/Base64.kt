package elide.util

import kotlinx.browser.window

/** Cross-platform utilities for encoding and decoding to/from Base64 (WASM-specialized). */
@Suppress("unused", "MemberVisibilityCanBePrivate") public actual object Base64: Encoder {
  /** @inheritDoc */
  override fun encoding(): Encoding {
    return Encoding.BASE64
  }

  // -- Base64: Encoding -- //
  /**
   * Encode the provided [string] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param string String to encode with Base64.
   * @return Base64-encoded string.
   */
  actual override fun encode(string: String): ByteArray {
    TODO("not yet implemented")
  }

  /**
   * Encode the provided [string] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param string String to encode with Base64.
   * @return Base64-encoded string.
   */
  actual override fun encodeToString(string: String): String {
    TODO("not yet implemented")
  }

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, which includes padding if necessary.
   *
   * @param data Raw bytes to encode with Base64.
   * @return Base64-encoded bytes.
   */
  actual override fun encode(data: ByteArray): ByteArray {
    TODO("not yet implemented")
  }

  /**
   * Encode the provided [data] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param data Raw bytes to encode into a Base64 string.
   * @return Base64-encoded string.
   */
  actual override fun encodeToString(data: ByteArray): String {
    TODO("not yet implemented")
  }

  // -- Base64: Encoding (Web-safe) -- //

  /**
   * Encode the provided [string] into a Base64-encoded string, omitting characters which are unsafe for use on the web,
   * including padding characters, which are not emitted.
   *
   * @param string String to encode with web-safe Base64.
   * @return Base64-encoded string, using only web-safe characters.
   */
  public actual fun encodeWebSafe(string: String): String {
    TODO("not yet implemented")
  }

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the
   * web, including padding characters, which are not emitted.
   *
   * @param data Raw bytes to encode with web-safe Base64.
   * @return Base64-encoded bytes, using only web-safe characters.
   */
  public actual fun encodeWebSafe(data: ByteArray): ByteArray {
    TODO("not yet implemented")
  }

  // -- Base64: Decoding -- //

  /**
   * Decode the provided [data] from Base64, returning a raw set of bytes resulting from the decoding operation.
   *
   * @param data Data to decode from Base64.
   * @return Raw bytes of decoded data.
   */
  actual override fun decode(data: ByteArray): ByteArray {
    TODO("not yet implemented")
  }

  /**
   * Decode the provided [string] from Base64, returning a raw set of bytes resulting from the decoding operation.
   *
   * @param string String to decode from Base64.
   * @return Raw bytes of decoded data.
   */
  actual override fun decode(string: String): ByteArray {
    TODO("not yet implemented")
  }

  /**
   * Decode the provided [data] from Base64, returning a regular string value, encoded as UTF-8.
   *
   * @param data Data to decode from Base64.
   * @return Decoded string value.
   */
  actual override fun decodeToString(data: ByteArray): String {
    TODO("not yet implemented")
  }

  /**
   * Decode the provided [string] from Base64, returning a regular string value, encoded as UTF-8.
   *
   * @param string String to decode from Base64.
   * @return Decoded string value.
   */
  actual override fun decodeToString(string: String): String {
    TODO("not yet implemented")
  }
}
