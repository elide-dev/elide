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
package elide.tooling

import kotlinx.serialization.Serializable

/**
 * ## Argument
 *
 * Describes the concept of a single "argument" to a command-line tool; such instances must ultimately be formatted as
 * string values. Argument instances may represent more than one actual command-line string argument; they can "expand"
 * into multiple arguments at formatting time.
 *
 * One use case for this is key-value arguments. With some calling conventions, an argument of `--key=value` is not
 * equivalent to `--key value` or other forms. Argument instances can remain aware of their key/value structure to allow
 * rendering phase code to account for this.
 */
@Serializable
public sealed interface Argument : Arguments {
  /**
   * Format this object as a command-line argument string.
   *
   * @return String representation of this argument, suitable for use in a command line.
   */
  public fun ArgumentContext.asArgumentString(): String

  /**
   * ## String Argument
   *
   * Simple one-value string argument.
   */
  @JvmInline public value class StringArg internal constructor(private val value: String) : Argument {
    override fun ArgumentContext.asArgumentString(): String = value
    override fun asArgumentSequence(): Sequence<Argument> = sequenceOf(this)
  }

  /**
   * ## Key-Value Argument
   *
   * Argument with a key and a value.
   */
  @JvmInline public value class KeyValueArg internal constructor(private val pair: Pair<String, String>) : Argument {
    public val name: String get() = pair.first
    public val value: String get() = pair.second
    override fun ArgumentContext.asArgumentString(): String = "${pair.first}${kvToken}${pair.second}"
    override fun asArgumentSequence(): Sequence<Argument> = sequenceOf(this)
  }

  /** Factories for arguments. */
  public companion object {
    /** Create a simple string argument. */
    @JvmStatic public fun of(item: String): Argument = StringArg(item)

    /** Create a pair for argument K/V (`k=v`). */
    @JvmStatic public fun of(pair: Pair<String, String>): Argument = KeyValueArg(pair)
  }
}
