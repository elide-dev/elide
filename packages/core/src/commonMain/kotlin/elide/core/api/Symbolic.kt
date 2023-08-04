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

package elide.core.api

/**
 * # Symbolic
 *
 * This interface maps a type [T] to the type to which it is applied; then, the adhering type's `companion object` (or
 * any sensible `object` related to type [T]) can implement [Symbolic.Resolver] to provide a means of resolving the
 * symbolic type to and from the concrete type.
 */
public interface Symbolic<T> {
  /**
   * ## Symbolic: Unresolved
   *
   * Thrown when a symbol could not be resolved, usually via the [SealedResolver.unresolved] method; this method is
   * typically used from the resolver implementation only.
   */
  public class Unresolved internal constructor (internal val requested: Any) : IllegalStateException()

  /** Return the raw symbol represented by this type. */
  public val symbol: T

  /**
   * ## Symbolic: Resolver
   *
   * Specifies the expected interface for a resolver of a symbolic type [T] to a concrete type [Concrete]. Implementors
   * inheriting from this interface are allowed to produce `null` for a symbol; because of previous design decisions,
   * the [resolveSymbol] method (deprecated) will throw an [Unresolved] exception if the symbol cannot be resolved.
   *
   * A "sealed resolver" will throw an exception for all unresolved symbols.
   *
   * @param T Symbolic type which can be resolved, by this resolver, to a [Concrete] type.
   * @param Concrete Concrete type which can be resolved from type [T].
   * @see [SealedResolver]
   */
  public sealed interface Resolver<T: Any, Concrete> where Concrete: Symbolic<T> {
    /**
     * ### Resolve Symbol
     *
     * Resolve the given symbol to a concrete type [Concrete], or `null` if the symbol cannot be resolved.
     *
     * @param symbol Symbol to resolve to a [Concrete] type.
     * @return Concrete result, or `null`.
     */
    @Throws(Unresolved::class)
    public fun resolve(symbol: T): Concrete?

    /**
     * ### Resolve a Symbol (Strict)
     *
     * Resolves a symbol or throws [Unresolved]; this interface is a hold-over from previous versions of Elide, and it
     * is marked for deletion in the next release.
     *
     * @deprecated Use [resolve] instead.
     */
    @Throws(Unresolved::class)
    @Deprecated("Use `resolve` instead", replaceWith = ReplaceWith("resolve"))
    public fun resolveSymbol(symbol: T): Concrete = resolve(symbol) ?: throw Unresolved(symbol)
  }

  /**
   * ## Symbolic: Enumerated
   *
   * Describes a [Symbolic] type which is "closed" or "sealed" to an enumerated set of instances or symbols. Resolvers
   * which inherit from this type will throw an exception ([Unresolved]) for all unresolved symbols.
   *
   * @param T Symbolic type which can be resolved, by this resolver, to a [Concrete] type.
   * @param Concrete Concrete type which can be resolved from type [T].
   */
  public sealed interface Enumerated<T: Any, Concrete>: Resolver<T, Concrete> where Concrete: Symbolic<T> {
    /**
     * ### Resolve Symbol
     *
     * Resolve the given symbol to a concrete type [Concrete], or `null` if the symbol cannot be resolved.
     *
     * @throws Unresolved if the symbol cannot be resolved.
     * @param symbol Symbol to resolve to a [Concrete] type.
     * @return Concrete result, or `null`.
     */
    @Throws(Unresolved::class)
    override fun resolve(symbol: T): Concrete

    /**
     * ### Unresolved Symbol
     *
     * Create an [Unresolved] exception for the given symbol; it is up to the caller to throw the exception so that
     * stacktrace info remains accurate.
     *
     * @param requested Requested symbol which could not be resolved.
     * @return Unresolved exception.
     */
    public fun unresolved(requested: T): Unresolved = Unresolved(requested)
  }

  /**
   * ## Symbolic: Sealed Resolver
   *
   * Describes a [Symbolic] resolver which is "sealed" or "closed;" requests for symbols which cannot be resolved will
   * result in an exception ([Unresolved]).
   *
   * @param T Symbolic type which can be resolved, by this resolver, to a [Concrete] type.
   * @param Concrete Concrete type which can be resolved from type [T].
   */
  public interface SealedResolver<T: Any, Concrete>: Enumerated<T, Concrete> where Concrete: Symbolic<T>

  /**
   * ## Symbolic: Abstract Resolver
   *
   * Describes an advanced resolver which operates on a factory pattern, and which carries a name. Names of resolvers
   * implemented via this class are used in logging, errors, and other cosmetic places.
   *
   * @param T Symbolic type which can be resolved, by this resolver.
   */
  public abstract class AbstractResolver<T>: SealedResolver<T, Symbolic<T>> where T: Symbolic<T> {
    /**
     * Return the name of this resolver.
     */
    public abstract val name: String
  }
}
