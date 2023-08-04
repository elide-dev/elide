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
import elide.data.HashAlgorithm

/**
 * TBD.
 */
public enum class KxHashAlgorithm (override val symbol: Int) : Symbolic<Int> {
  /** Algorithm: No algorithm applied (just trimmed data). */
  IDENTITY(symbol = HashAlgorithm.IDENTITY.ordinal),

  /** Algorithm: MD5. */
  MD5(symbol = HashAlgorithm.MD5.ordinal),

  /** Algorithm: SHA-1. */
  SHA1(symbol = HashAlgorithm.SHA1.ordinal),

  /** Algorithm: SHA-256. */
  SHA256(symbol = HashAlgorithm.SHA_256.ordinal),

  /** Algorithm: SHA-512. */
  SHA512(symbol = HashAlgorithm.SHA_512.ordinal),

  /** Algorithm: SHA3-224. */
  SHA3_224(symbol = HashAlgorithm.SHA3_224.ordinal),

  /** Algorithm: SHA3-256. */
  SHA3_256(symbol = HashAlgorithm.SHA3_256.ordinal),

  /** Algorithm: SHA3-512. */
  SHA3_512(symbol = HashAlgorithm.SHA3_512.ordinal);

  /** Resolves integer symbols to [KxHashAlgorithm] enumeration entries. */
  public companion object : SealedResolver<Int, KxHashAlgorithm> {
    /** @return [KxHashAlgorithm] for the provided raw integer [symbol]. */
    @JvmStatic override fun resolve(symbol: Int): KxHashAlgorithm = when (symbol) {
      HashAlgorithm.IDENTITY.ordinal -> IDENTITY
      HashAlgorithm.MD5.ordinal -> MD5
      HashAlgorithm.SHA1.ordinal -> SHA1
      HashAlgorithm.SHA_256.ordinal -> SHA256
      HashAlgorithm.SHA_512.ordinal -> SHA512
      HashAlgorithm.SHA3_224.ordinal -> SHA3_224
      HashAlgorithm.SHA3_256.ordinal -> SHA3_256
      HashAlgorithm.SHA3_512.ordinal -> SHA3_512
      else -> throw unresolved(symbol)
    }
  }
}
