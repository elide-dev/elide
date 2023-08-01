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
 * # Encoded Data
 *
 * Specifies the interface which encoded data value classes comply with; each encoded data class indicates its encoding,
 * and provides a means for decoding to/from strings.
 *
 * @param T Underlying data type. Either a [ByteArray] or [String].
 */
public interface EncodedData {
  /** Indicate the encoding type applied to the data held by this object. */
  public val encoding: Encoding

  /** Raw data associated with this encoded data payload. */
  public val data: ByteArray

  /** Decode the underlying data to a string. */
  public val string: String
}
