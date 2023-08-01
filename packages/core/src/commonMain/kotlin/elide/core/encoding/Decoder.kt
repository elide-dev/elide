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

package elide.core.encoding

/**
 * # Decoder
 */
public interface Decoder<Raw : EncodedData> : CodecIdentifiable {
  /**
   * Decode the provided [data] from the [encoding] implemented by this encoder; return a raw [ByteArray] of the
   * resulting bytes.
   *
   * @param data Data to decode using this adapter.
   * @return Raw bytes decoded from [data].
   */
  public fun decodeBytes(data: ByteArray): ByteArray

  /**
   * Decode the provided raw [data] from the [encoding] implemented by this encoder; return a raw [ByteArray] of the
   * resulting data.
   *
   * @param data Encoded data to decode.
   * @return Decoded raw data array.
   */
  public fun decode(data: Raw): ByteArray = decodeBytes(data.data)

  /**
   * Decode the provided [string] using the [encoding] implemented by this encoder; return a raw [ByteArray] of the
   * resulting bytes.
   *
   * @param string String to decode using this adapter.
   * @return Raw bytes decoded from [string].
   */
  public fun decodeString(string: String): ByteArray = decodeBytes(string.encodeToByteArray())

  /**
   * Decode the provided [data] from the [encoding] implemented by this encoder; return a [String] representation of the
   * resulting data.
   *
   * @param data Encoded data to decode.
   * @return Decoded string representation of the data.
   */
  public fun decodeToString(data: ByteArray): String = decodeBytes(data).decodeToString()

  /**
   * Decode the provided [string] from the [encoding] implemented by this encoder; return a [String] representation of
   * the resulting data.
   *
   * @param string Encoded string to decode.
   * @return Decoded string representation of the data.
   */
  public fun decodeToString(string: String): String = decodeBytes(string.encodeToByteArray()).decodeToString()
}
