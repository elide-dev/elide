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

package elide.core.encoding.hex

import elide.core.encoding.Codec
import elide.core.encoding.Encoding

/**
 * # Hex
 *
 * Provides cross-platform utilities for encoding values into hex, or decoding values from hex. Available on any target
 * platform supported by Elide/Kotlin, including native platforms.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") public object DefaultHex : Codec<HexData> {
  /** Array of hex-allowable characters.  */
  public val CHARACTER_SET: CharArray = "0123456789abcdef".toCharArray()

  // Convert a byte array to a hex-encoded string.
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
  override fun encoding(): Encoding = Encoding.HEX

  /** @inheritDoc */
  override fun encode(data: ByteArray): HexData {
    return HexData(bytesToHex(data))
  }

  /** @inheritDoc */
  override fun decodeBytes(data: ByteArray): ByteArray = hexToString(data.decodeToString()).encodeToByteArray()
}
