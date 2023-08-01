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

import kotlin.jvm.JvmInline
import elide.core.encoding.EncodedData
import elide.core.encoding.Encoding

/** Carrier value-class for hex-encoded data. */
@JvmInline public value class HexData(private val encoded: String) : EncodedData {
  /** @inheritDoc */
  override val encoding: Encoding get() = Encoding.HEX

  /** @inheritDoc */
  override val string: String get() = encoded

  /** @inheritDoc */
  override val data: ByteArray get() = encoded.encodeToByteArray()
}
