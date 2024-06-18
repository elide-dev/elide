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

package elide.runtime.gvm.internals.intrinsics.js

import kotlinx.serialization.Serializable
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.Symbol

/**
 * # JavaScript Symbol
 *
 * Describes a symbol registered for injection into the JavaScript VM as an intrinsic value; JavaScript symbols consist
 * of a name and an "internal" flag.
 *
 * The name of a symbol becomes the value's name within an executing VM; the internal flag indicates whether the value
 * should be made available to user code, or merely to internal scripts as a "primordial value."
 *
 * &nsbp;
 *
 * ## Primordial Values
 *
 * Symbols marked as internal are not guaranteed to be provided to user code, and may carry a prefix which obfuscates
 * their use as public APIs.
 *
 * Such symbols will return `true` from [isInternal].
 */
@Serializable
@JvmInline internal value class JsSymbol private constructor (private val data: Pair<String, Boolean>) :
  Symbol,
  Comparable<Symbol>,
  java.io.Serializable {

  /** Public constructor. */
  constructor(name: String, internal: Boolean = true) : this(name to internal)

  /** Indicate whether this symbol is "internal," in which case, it should not be provided to user code. */
  override val isInternal: Boolean get() = data.second

  /** Describes the "internal symbol" name, made available via the primordial object. */
  override val internalSymbol: String get() = data.first

  override val symbol: String get() = isInternal.let {
    checkSymbol(it, if (it) internalSymbol(data.first) else data.first)
  }

  /** Well-known JavaScript symbols. */
  internal companion object JsSymbols {
    private const val ELIDE_INTERNAL_PREFIX = "__Elide_"

    // Render an internal symbol.
    private fun internalSymbol(name: String): String = "$ELIDE_INTERNAL_PREFIX${name}__"

    private fun checkSymbol(isInternal: Boolean, symbol: String): String {
      require(symbol.isNotEmpty()) { "Symbol must not be empty" }
      if (!isInternal) require(!symbol.contains(ELIDE_INTERNAL_PREFIX)) {
        "Non-internal symbol must not contain the reserved prefix '$ELIDE_INTERNAL_PREFIX'"
      }
      return symbol
    }

    /** @return JS symbol wrapping the receiving string. */
    @DelicateElideApi @JvmStatic fun String.asPublicJsSymbol(): JsSymbol = asJsSymbol(false)

    /** @return JS symbol wrapping the receiving string. */
    @JvmStatic fun String.asJsSymbol(internal: Boolean = true): JsSymbol = JsSymbol(this to internal)
  }

  /** @inheritDoc */
  override fun toString(): String = "Symbol{$symbol}"

  /** @inheritDoc */
  override fun compareTo(other: Symbol): Int = this.symbol.compareTo(other.symbol)
}
