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
 * # Encoder
 */
public interface Encoder<Raw : EncodedData> : CodecIdentifiable {
  // -- Methods: Encoding -- //
  /**
   * Encoded the provided [data] using the [encoding] implemented by this encoder; the returned [Raw] instance is able
   * to provide a resulting [ByteArray] or [String].
   *
   * @param data Data to encode with this encoder.
   * @return Raw encoded data record.
   */
  public fun encode(data: ByteArray): Raw

  /**
   * Encode the provided [data] using the [encoding] implemented by this encoder; return a [ByteArray] representation
   * of the result.
   *
   * @param data Data to encode with this encoder.
   * @return Raw encoded output data.
   */
  public fun encodeBytes(data: ByteArray): ByteArray = encode(data).data

  /**
   * Encode the provided [string] data using the [encoding] implemented by this encoder; by default, the string will
   * be interpreted using `UTF-8` encoding, then encoded to the target encoding.
   *
   * @param string String to encode with this encoder.
   * @return Encoded bytes of the provided string.
   */
  public fun encodeString(string: String): ByteArray = encode(string.encodeToByteArray()).data

  /**
   * Encode the provided [data] to a string representation using the [encoding] implemented by this encoder.
   *
   * @param data Raw data to encode in the target encoding and return as a string.
   * @return String representation of the encoded data.
   */
  public fun encodeToString(data: ByteArray): String = encode(data).string

  /**
   * Encode the provided [string] to a string representation using the [encoding] implemented by this encoder; the
   * string is interpreted using `UTF-8` encoding.
   *
   * @param string String to encode in the target encoding and return as a string.
   * @return String representation of the encoded data.
   */
  public fun encodeToString(string: String): String = encode(string.encodeToByteArray()).string
}
