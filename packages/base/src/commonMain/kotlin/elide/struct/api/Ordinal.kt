/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.struct.api

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

/**
 *
 */
@JvmInline public value class Ordinal private constructor (internal val index: UInt) : Comparable<Ordinal> {
  /** */
  public companion object {
    /**
     *
     */
    @JvmStatic public fun of(index: Int): Ordinal = index.let {
      require(it >= 0) { "Ordinal index must be non-negative." }
      Ordinal(it.toUInt())
    }

    /**
     *
     */
    @JvmStatic public fun of(index: UInt): Ordinal = Ordinal(index)

    /**
     *
     */
    @JvmStatic public fun Int.asOrdinal(): Ordinal = of(this)

    /**
     *
     */
    @JvmStatic public fun UInt.asOrdinal(): Ordinal = of(this)
  }

  override fun compareTo(other: Ordinal): Int {
    return index.compareTo(other.index)
  }
}
