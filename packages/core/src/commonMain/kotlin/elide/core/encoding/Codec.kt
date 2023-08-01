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
 * # Codec
 *
 * Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or
 * expression, as well as decoding from that same format.
 *
 * @param Raw Value class which wraps values produced by this encoder.
 * @see EncodedData for the interface each encoded-data record complies with.
 * @see Decoder for the interface description for decoding.
 * @see Encoding for the interface description for encoding.
 */
public interface Codec<Raw : EncodedData> : Encoder<Raw>, Decoder<Raw>, CodecIdentifiable {
  /**
   * ## Decoder
   *
   * Provide a [Decoder] specialized to the format implemented by this [Codec]; the resulting object can only guarantee
   * a capability to decode data from the subject encoding.
   */
  public fun encoder(): Encoder<Raw> {
    return this
  }

  /**
   * ## Encoder
   *
   * Provide an [Encoder] specialized to the format implemented by this [Codec]; the resulting object can only guarantee
   * a capability to encode data to the target encoding.
   */
  public fun decoder(): Decoder<Raw> {
    return this
  }
}
