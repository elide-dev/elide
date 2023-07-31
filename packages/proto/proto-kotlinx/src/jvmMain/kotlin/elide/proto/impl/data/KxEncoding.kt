@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl.data

import elide.data.Encoding
import elide.proto.api.Symbolic

/** TBD. */
public enum class KxEncoding constructor (override val symbol: Int) : Symbolic<Int> {
  /** Hex encoding. */
  HEX(symbol = Encoding.HEX.ordinal),

  /** Base64 encoding. */
  BASE64(symbol = Encoding.BASE64.ordinal);

  /** Resolves integer symbols to [KxEncoding] enumeration entries. */
  public companion object : Symbolic.Resolver<Int, KxEncoding> {
    /** @return [KxEncoding] for the provided raw integer [symbol]. */
    @JvmStatic override fun resoleSymbol(symbol: Int): KxEncoding = when (symbol) {
      Encoding.HEX.ordinal -> HEX
      Encoding.BASE64.ordinal -> BASE64
      else -> throw unresolved(symbol)
    }
  }
}
