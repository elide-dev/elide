@file:Suppress("unused")

package elide.core.encoding

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.base64.DefaultBase64
import elide.core.encoding.hex.DefaultHex
import elide.core.encoding.hex.HexData


// -- Base64: Encoding -- //

/**
 * Encode the current [ByteArray] to a Base64 string, using the cross-platform [DefaultBase64] tools.
 *
 * @return Base64 encoded string from the current set of bytes.
 */
public fun ByteArray.toBase64String(): String {
  return DefaultBase64.encodeToString(this)
}

/**
 * Encode the current [ByteArray] to a Base64 byte array wrapped using an encoded data record.
 *
 * @return Base64 encoded bytes from the current set of bytes, wrapped in an encoded data record.
 */
public fun ByteArray.toBase64(): Base64Data {
  return DefaultBase64.encode(this)
}

/**
 * Encode the current [ByteArray] to a Base64 byte array, using the cross-platform [DefaultBase64] tools.
 *
 * @return Base64 encoded bytes from the current set of bytes.
 */
public fun ByteArray.toBase64Bytes(): ByteArray {
  return DefaultBase64.encodeBytes(this)
}

/**
 * Encode the current [String] to a Base64 string, using the cross-platform [DefaultBase64] tools.
 *
 * @return Base64 encoded string from the current string.
 */
public fun String.toBase64String(): String {
  return DefaultBase64.encodeToString(this)
}

/**
 * Encode the current [String] to a Base64 byte array, using the cross-platform [DefaultBase64] tools.
 *
 * @return Base64 encoded byte array from the current string.
 */
public fun String.toBase64(): Base64Data {
  return DefaultBase64.encode(this.encodeToByteArray())
}

/**
 * Encode the current [String] to a Base64 byte array, using the cross-platform [DefaultBase64] tools.
 *
 * @return Base64 encoded byte array from the current string.
 */
public fun String.toBase64Bytes(): ByteArray {
  return DefaultBase64.encodeString(this)
}

// -- Hex: Encoding -- //

/**
 * Encode the current [ByteArray] to a hex-encoded string, wrapped in a [HexData] record.
 *
 * @return Hex-encoded string from the current set of bytes, wrapped in an encoded data record.
 */
public fun ByteArray.toHex(): HexData {
  return DefaultHex.encode(this)
}

/**
 * Encode the current [ByteArray] to a hex-encoded string, using the cross-platform [DefaultHex] tools.
 *
 * @return Hex-encoded string from the current set of bytes.
 */
public fun ByteArray.toHexString(): String {
  return DefaultHex.encodeToString(this)
}

/**
 * Encode the current [ByteArray] to a hex-encoded byte array, using the cross-platform [DefaultHex] tools.
 *
 * @return Hex-encoded byte array from the current set of bytes.
 */
public fun ByteArray.toHexBytes(): ByteArray {
  return DefaultHex.encodeBytes(this)
}

/**
 * Encode the current [String] to a hex-encoded string, wrapped in a [HexData] record.
 *
 * @return Hex-encoded string from the current string, wrapped in an encoded data record.
 */
public fun String.toHex(): HexData {
  return DefaultHex.encode(this.encodeToByteArray())
}

/**
 * Encode the current [String] to a hex-encoded string, using the cross-platform [DefaultHex] tools.
 *
 * @return Hex-encoded string from the current string.
 */
public fun String.toHexString(): String {
  return DefaultHex.encodeToString(this)
}

/**
 * Encode the current [String] to a hex-encoded byte array, using the cross-platform [DefaultHex] tools.
 *
 * @return Hex-encoded byte array from the current string.
 */
public fun String.toHexBytes(): ByteArray {
  return DefaultHex.encodeBytes(this.encodeToByteArray())
}
