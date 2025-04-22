/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.tool

import kotlinx.collections.immutable.PersistentList

/**
 * ## Mutable Arguments
 *
 * Extends the base concept of program [Arguments] with mutability; this interface allows arguments to be added in
 * various forms. "Building" a mutable argument suite results in a regular [Arguments] suite; calling
 * [Arguments.Suite.toMutable] produces an instance of [MutableArguments].
 *
 * If an arguments container is already mutable, calls to [Arguments.Suite.toMutable] return the same instance.
 */
public sealed interface MutableArguments : Arguments, Arguments.Suite {
  override fun toMutable(): MutableArguments = this

  /**
   * Build this suite of mutable arguments into a finalized immutable set of arguments.
   *
   * @return Finalized immutable arguments.
   */
  public fun build(): Arguments

  /**
   *
   */
  public fun add(element: Argument): Boolean

  /**
   *
   */
  public fun add(element: String): Boolean = add(Argument.of(element))

  /**
   *
   */
  public fun addAll(elements: Collection<Argument>): Boolean

  /**
   *
   */
  public fun addAllStrings(elements: Collection<String>): Boolean = addAll(elements.map { Argument.of(it) })

  @JvmInline public value class MutableArgumentList(
    private val held: MutableList<Argument>,
  ) : MutableArguments, MutableList<Argument> by held {
    override fun asArgumentSequence(): Sequence<Argument> = held.asSequence()
    override fun get(index: Int): Argument = held[index]
    override fun build(): Arguments = Arguments.of(held.asSequence())
    override fun add(element: Argument): Boolean = held.add(element)
    override fun addAll(elements: Collection<Argument>): Boolean = held.addAll(elements)
  }

  public class PersistentArgumentList(
    private var held: PersistentList<Argument>,
  ) : MutableArguments {
    override fun asArgumentSequence(): Sequence<Argument> = held.asSequence()
    override fun get(index: Int): Argument = held[index]
    override fun build(): Arguments = Arguments.of(held.asSequence())
    override fun contains(element: Argument): Boolean = held.contains(element)
    override fun containsAll(elements: Collection<Argument>): Boolean = held.containsAll(elements)
    override fun isEmpty(): Boolean = held.isEmpty()
    override fun iterator(): MutableIterator<Argument> = held.toMutableList().iterator()
    override val size: Int get() = held.size
    override fun add(element: Argument): Boolean = true.also { held = held.add(element) }
    override fun addAll(elements: Collection<Argument>): Boolean = true.also {
      held = held.addAll(elements)
    }
  }

  /** Factories for creating or obtaining mutable argument sets. */
  public companion object {
    /** Default clone function for creating a mutable suite of arguments. */
    @JvmStatic public fun from(other: Arguments): MutableArguments = when (other) {
      is MutableArguments -> other
      is Arguments.Suite -> MutableArgumentList(held = other.toMutableList())
      else -> MutableArgumentList(held = other.asArgumentSequence().toMutableList())
    }

    /** Create a mutable argument suite from a persistent list of arguments; internal use only. */
    @JvmStatic public fun from(other: PersistentList<Argument>): MutableArguments = PersistentArgumentList(held = other)
  }
}
