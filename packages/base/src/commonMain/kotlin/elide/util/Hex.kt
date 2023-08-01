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

/** Provides cross-platform utilities for encoding values into hex, or decoding values from hex. */
public object Hex : Encoder {
  /** Array of hex-allowable characters.  */
  public val CHARACTER_SET: CharArray = "0123456789abcdef".toCharArray()

  /**
   * Convert a byte array to a hex-encoded string.
   *
   * @param bytes Raw bytes to encode.
   * @return Resulting hex-encoded string.
   */
  internal fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
      val v = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = CHARACTER_SET[v ushr 4]
      hexChars[j * 2 + 1] = CHARACTER_SET[v and 0x0F]
    }
    return hexChars.concatToString().trim { it <= ' ' }.replace("\u0000", "")
  }

  // Internal implementation of hex decoding from strings to strings.
  internal fun hexToString(hex: String): String {
    val result = ByteArray(hex.length / 2)
    for (i in hex.indices step 2) {
      val firstIndex = CHARACTER_SET.indexOf(hex[i])
      val secondIndex = CHARACTER_SET.indexOf(hex[i + 1])
      val octet = firstIndex.shl(4).or(secondIndex)
      result[i.shr(1)] = octet.toByte()
    }
    return result.decodeToString()
  }

  /** @inheritDoc */
  override fun encoding(): Encoding {
    return Encoding.HEX
  }

  /**
   * Encode the provided [data] as a byte array of hex-encoded data.
   *
   * @param data Data to encode with hex.
   * @return Data encoded with hex.
   */
  override fun encode(data: ByteArray): ByteArray {
    return bytesToHex(data).encodeToByteArray()
  }

  /**
   * Encode the provided [string] as a byte array of hex-encoded data.
   *
   * @param string String to encode with hex.
   * @return Data encoded with hex.
   */
  override fun encode(string: String): ByteArray {
    return encode(string.encodeToByteArray())
  }

  /**
   * Encode the provided [data] as a string of hex-encoded data.
   *
   * @param data Data to encode with hex.
   * @return String encoded with hex.
   */
  override fun encodeToString(data: ByteArray): String {
    return bytesToHex(data)
  }

  /**
   * Encode the provided [string] as a string of hex-encoded data.
   *
   * @param string String to encode with hex.
   * @return String encoded with hex.
   */
  override fun encodeToString(string: String): String {
    return encode(
      string.encodeToByteArray(),
    ).decodeToString()
  }

  /**
   * Decode the provided [data] as a byte array of hex-encoded data.
   *
   * @param data Data to decode with hex.
   * @return Raw data decoded from hex.
   */
  override fun decode(data: ByteArray): ByteArray {
    return hexToString(
      data.decodeToString(),
    ).encodeToByteArray()
  }

  /**
   * Decode the provided [string] as a byte array of hex-encoded data.
   *
   * @param string String to decode with hex.
   * @return Raw data decoded from hex.
   */
  override fun decode(string: String): ByteArray {
    return hexToString(string).encodeToByteArray()
  }

  /**
   * Decode the provided [data] into a string of decoded data.
   *
   * @param data Data to decode with hex, into a string.
   * @return String decoded from hex.
   */
  override fun decodeToString(data: ByteArray): String {
    return hexToString(data.decodeToString())
  }

  /**
   * Decode the provided [string] into a string of decoded data.
   *
   * @param string String decode with hex, into a string.
   * @return String decoded from hex.
   */
  override fun decodeToString(string: String): String {
    return hexToString(string)
  }
}
