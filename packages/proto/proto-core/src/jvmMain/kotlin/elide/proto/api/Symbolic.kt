@file:Suppress("RedundantVisibilityModifier")

package elide.proto.api

/**
 * TBD.
 */
public interface Symbolic<T> {
  /** Thrown when a symbol could not be resolved. */
  public class SymbolUnresolved internal constructor (internal val requested: Any) : IllegalStateException()

  /** Return the raw symbol represented by this type. */
  val symbol: T

  // Nothing yet.
  public interface Resolver<Symbol: Any, Concrete> where Concrete: Symbolic<Symbol> {
    /**
     * TBD.
     */
    @Throws(SymbolUnresolved::class)
    public fun resoleSymbol(symbol: Symbol): Concrete

    /** @return Exception instance for an unresolved symbol. */
    fun unresolved(requested: Any) = SymbolUnresolved(requested)
  }
}
