@file:Suppress("unused")

package elide.util


// -- Base64: Encoding -- //

/**
 * Encode the current [ByteArray] to a Base64 string, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded string from the current set of bytes.
 */
fun ByteArray.toBase64String(): String {
  return Base64.encodeToString(this)
}

/**
 * Encode the current [ByteArray] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded bytes from the current set of bytes.
 */
fun ByteArray.toBase64(): ByteArray {
  return Base64.encode(this)
}

/**
 * Encode the current [String] to a Base64 string, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded string from the current string.
 */
fun String.toBase64String(): String {
  return Base64.encodeToString(this)
}

/**
 * Encode the current [String] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded byte array from the current string.
 */
fun String.toBase64(): ByteArray {
  return Base64.encode(this)
}

// -- Hex: Encoding -- //

/**
 * Encode the current [String] to a hex-encoded string, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded string from the current string.
 */
fun String.toHexString(): String {
  return Hex.encodeToString(this)
}

/**
 * Encode the current [String] to a hex-encoded byte array, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded byte array from the current string.
 */
fun String.toHex(): ByteArray {
  return Hex.encode(this)
}
