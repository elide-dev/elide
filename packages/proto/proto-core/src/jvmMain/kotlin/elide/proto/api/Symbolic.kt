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

@file:Suppress(
  "RedundantVisibilityModifier",
  "DEPRECATION",
)

package elide.proto.api

/**
 * TBD.
 */
@Deprecated(
  "`Symbolic` has moved to `elide-core`",
  replaceWith = ReplaceWith(
    "elide.core.api.Symbolic",
    "elide.core.api.Symbolic",
  ),
)
public interface Symbolic<T> {
  /** Thrown when a symbol could not be resolved. */
  @Deprecated(
    "`Symbolic.SymbolUnresolved` has moved to `elide-core`",
    replaceWith = ReplaceWith(
      "elide.core.api.Symbolic.Unresolved",
      "elide.core.api.Symbolic.Unresolved",
    ),
  )
  public class SymbolUnresolved internal constructor (internal val requested: Any) : IllegalStateException()

  /** Return the raw symbol represented by this type. */
  val symbol: T

  @Deprecated(
    "`Symbolic.Resolver` has moved to `elide-core`",
    replaceWith = ReplaceWith(
      "elide.core.api.Symbolic.SealedResolver",
      "elide.core.api.Symbolic.SealedResolver",
    ),
  )
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
