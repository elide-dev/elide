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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl.data

import elide.core.api.Symbolic
import elide.core.api.Symbolic.SealedResolver
import elide.data.Encoding

/** TBD. */
public enum class KxEncoding (override val symbol: Int) : Symbolic<Int> {
  /** Hex encoding. */
  HEX(symbol = Encoding.HEX.ordinal),

  /** Base64 encoding. */
  BASE64(symbol = Encoding.BASE64.ordinal);

  /** Resolves integer symbols to [KxEncoding] enumeration entries. */
  public companion object : SealedResolver<Int, KxEncoding> {
    /** @return [KxEncoding] for the provided raw integer [symbol]. */
    @JvmStatic override fun resolve(symbol: Int): KxEncoding = when (symbol) {
      Encoding.HEX.ordinal -> HEX
      Encoding.BASE64.ordinal -> BASE64
      else -> throw unresolved(symbol)
    }
  }
}
