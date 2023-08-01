/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.util

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
 * Encode the current [ByteArray] to a Base64 byte array, using the cross-platform [Base64] tools.
 *
 * @return Base64 encoded bytes from the current set of bytes.
 */
public fun ByteArray.toBase64(): ByteArray {
  return Base64.encode(this)
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
public fun String.toBase64(): ByteArray {
  return Base64.encode(this)
}

// -- Hex: Encoding -- //

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
public fun String.toHex(): ByteArray {
  return Hex.encode(this)
}
