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
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

/**
 * # Arguments
 *
 * Defines the concept of program arguments, which are gathered and then passed ultimately to some other tool; arguments
 * have many forms, including simple strings, key-value pairs, and more complex structures like file paths. Arguments
 * and their expression may depend on the target tool or host operating system.
 *
 * Inherently, an [Arguments] instance is a container of [Argument] instances. Arguments in plural form may be backed by
 * nested or repeated structures. When rendered for the command-line, all held arguments are flattened and expressed.
 *
 * @see MutableArguments Mutable containers of arguments
 * @see Argument `Argument` interface
 * @see Tool `Tool` interface, which uses `Arguments`
 */
@Serializable
public sealed interface Arguments {
  /**
   * Format this object as a sequence of command-line arguments.
   *
   * @return Sequence of strings representing this argument, suitable for use in a command line.
   */
  public fun asArgumentSequence(): Sequence<Argument>

  /**
   * Build a list from this suite of arguments.
   *
   * @return List of strings representing this argument, suitable for use in a command line.
   */
  public fun asArgumentList(): List<String> = asArgumentStrings().toList()

  /**
   * Format this object as a sequence of command-line arguments.
   *
   * @return Sequence of strings representing this argument, suitable for use in a command line.
   */
  public fun asArgumentStrings(): Sequence<String> = asArgumentSequence()
    .flatMap { it.asArgumentSequence() }
    .map { it.asArgumentString() }

  /**
   * Get the argument at the positional [index], or throw.
   *
   * @param index Index of the argument to retrieve.
   * @return The argument at the specified index.
   * @throws IndexOutOfBoundsException if the index is out of bounds.
   */
  public operator fun get(index: Int): Argument = asArgumentSequence().elementAt(index)

  /**
   * ## Arguments Suite
   *
   * Defines a suite of arguments, which extends the base concept of [Arguments] with collection-like behavior:
   *
   * - The count of arguments is known ahead of time
   * - The arguments are all instances of [Argument]
   * - The arguments are expressed immutably with support for [MutableArguments]
   */
  public sealed interface Suite : Arguments, Collection<Argument> {
    /**
     * Mutable Arguments.
     *
     * Provide a mutable form of this arguments container, pre-initialized with matching values.
     *
     * @return A mutable arguments container, initialized with the same values as this instance.
     */
    public fun toMutable(): MutableArguments = MutableArguments.from(this)
  }

  /**
   * ### Single Argument
   *
   * Defines an argument suite with a single argument value.
   */
  @JvmInline public value class SingleArgument internal constructor (private val single: Argument) : Suite {
    override fun asArgumentSequence(): Sequence<Argument> = sequenceOf(single)
    override fun contains(element: Argument): Boolean = single == element
    override fun containsAll(elements: Collection<Argument>): Boolean = elements.all { it == single }
    override fun isEmpty(): Boolean = false
    override val size: Int get() = 1
    override fun iterator(): Iterator<Argument> = sequenceOf(single).iterator()
  }

  /**
   * ### Argument List
   *
   * Defines an argument suite with a persistent list of args; the backing list is expressed as a [PersistentList], and
   * held in immutable form. Extending the list via [MutableArguments] is supported and efficient.
   */
  @JvmInline public value class ArgumentList internal constructor (
    private val held: PersistentList<Argument>
  ) : Suite, Collection<Argument> by held {
    override fun asArgumentSequence(): Sequence<Argument> = held.asSequence()
    override fun toMutable(): MutableArguments = MutableArguments.from(held)
  }

  /**
   * ### Empty Arguments
   *
   * Placeholder that defines non-null but empty arguments.
   */
  public data object EmptyArguments : Suite {
    override fun asArgumentSequence(): Sequence<Argument> = emptySequence()
    override fun contains(element: Argument): Boolean = false
    override fun containsAll(elements: Collection<Argument>): Boolean = false
    override fun isEmpty(): Boolean = true
    override val size: Int get() = 0
    override fun iterator(): Iterator<Argument> = emptySequence<Argument>().iterator()
  }

  /** Factories for producing [Arguments] instances. */
  public companion object {
    /** @return Empty arguments. */
    @JvmStatic public fun empty(): EmptyArguments = EmptyArguments

    /** @return Argument suite with a [single] backing argument. */
    @JvmStatic public fun of(single: String): Arguments = SingleArgument(Argument.of(single))

    /** @return Argument suite with a single [pair] argument representing a key and value. */
    @JvmStatic public fun of(pair: Pair<String, String>): Suite = SingleArgument(Argument.of(pair))

    /** @return Argument suite with a [sequence] of initial values. */
    @JvmStatic public fun from(sequence: Sequence<String>): Suite =
      ArgumentList(sequence.map { Argument.of(it) }.toPersistentList())

    /** @return Argument suite with a [collection] of initial values. */
    @JvmStatic public fun from(collection: Collection<String>): Suite =
      ArgumentList(collection.map { Argument.of(it) }.toPersistentList())

    /** @return Argument suite with a [collection] of initial values. */
    @JvmStatic public fun of(collection: Collection<Argument>): Suite = ArgumentList(collection.toPersistentList())

    /** @return Argument suite with a [collection] of initial values. */
    @JvmStatic public fun of(collection: Sequence<Argument>): Suite = ArgumentList(collection.toPersistentList())
  }
}
