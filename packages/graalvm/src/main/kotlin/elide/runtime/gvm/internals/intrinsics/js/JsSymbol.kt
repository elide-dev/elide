package elide.runtime.gvm.internals.intrinsics.js

import elide.runtime.intrinsics.js.Symbol
import kotlinx.serialization.Serializable

/**
 * TBD.
 */
@Serializable
@JvmInline internal value class JsSymbol (override val symbol: String) :
  Symbol,
  Comparable<Symbol>,
  java.io.Serializable {
  /** Well-known JavaScript symbols. */
  internal companion object JsSymbols {
    /** @return JS symbol wrapping the receiving string. */
    @JvmStatic fun String.asJsSymbol(): JsSymbol = JsSymbol(this)
  }

  /** @inheritDoc */
  override fun toString(): String = "Symbol{$symbol}"

  /** @inheritDoc */
  override fun compareTo(other: Symbol): Int = this.symbol.compareTo(other.symbol)
}
