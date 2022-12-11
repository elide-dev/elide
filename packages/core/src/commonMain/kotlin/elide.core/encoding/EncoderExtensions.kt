@file:Suppress("unused")

package elide.core.encoding


// -- Base64: Encoding -- //

/**
 * Encode the current [ByteArray] to a Base64 string, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded string from the current set of bytes.
 */
public fun ByteArray.toBase64String(): String {
  return Base64.encodeToString(this)
}

/**
 * Encode the current [ByteArray] to a Base64 byte array wrapped using an encoded data record.
 *
 * @return Base64 encoded bytes from the current set of bytes, wrapped in an encoded data record.
 */
public fun ByteArray.toBase64(): Base64.Base64Data {
  return Base64.encode(this)
}

/**
 * Encode the current [ByteArray] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded bytes from the current set of bytes.
 */
public fun ByteArray.toBase64Bytes(): ByteArray {
  return Base64.encodeBytes(this)
}

/**
 * Encode the current [String] to a Base64 string, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded string from the current string.
 */
public fun String.toBase64String(): String {
  return Base64.encodeToString(this)
}

/**
 * Encode the current [String] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded byte array from the current string.
 */
public fun String.toBase64(): Base64.Base64Data {
  return Base64.encode(this.encodeToByteArray())
}

/**
 * Encode the current [String] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded byte array from the current string.
 */
public fun String.toBase64Bytes(): ByteArray {
  return Base64.encodeString(this)
}

// -- Hex: Encoding -- //

/**
 * Encode the current [ByteArray] to a hex-encoded string, wrapped in a [Hex.HexData] record.
 *
 * @return Hex-encoded string from the current set of bytes, wrapped in an encoded data record.
 */
public fun ByteArray.toHex(): Hex.HexData {
  return Hex.encode(this)
}

/**
 * Encode the current [ByteArray] to a hex-encoded string, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded string from the current set of bytes.
 */
public fun ByteArray.toHexString(): String {
  return Hex.encodeToString(this)
}

/**
 * Encode the current [ByteArray] to a hex-encoded byte array, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded byte array from the current set of bytes.
 */
public fun ByteArray.toHexBytes(): ByteArray {
  return Hex.encodeBytes(this)
}

/**
 * Encode the current [String] to a hex-encoded string, wrapped in a [Hex.HexData] record.
 *
 * @return Hex-encoded string from the current string, wrapped in an encoded data record.
 */
public fun String.toHex(): Hex.HexData {
  return Hex.encode(this.encodeToByteArray())
}

/**
 * Encode the current [String] to a hex-encoded string, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded string from the current string.
 */
public fun String.toHexString(): String {
  return Hex.encodeToString(this)
}

/**
 * Encode the current [String] to a hex-encoded byte array, using the cross-platform [Hex] tools.
 *
 * @return Hex-encoded byte array from the current string.
 */
public fun String.toHexBytes(): ByteArray {
  return Hex.encodeBytes(this.encodeToByteArray())
}
