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

package elide.http

/**
 * ## HTTP Parameter Value
 *
 * Describes a single parameter value, which can be a single value (as in a single string) or a multi-value (as in a
 * repeated suite of values sharing a key).
 */
public sealed interface ParamValue : HttpToken {
  /**
   * The string value(s) of this parameter.
   */
  public val values: Sequence<String>

  /**
   * Regular string parameter value.
   *
   * @property value Singular value held by this parameter.
   */
  @JvmInline public value class StringParam internal constructor (internal val value: String) : ParamValue {
    override fun asString(): String = value
    override val values: Sequence<String> get() = sequenceOf(value)
  }

  /**
   * Regular string parameter value.
   *
   * @property value Sequence of values held by this parameter.
   */
  @JvmInline public value class MultiParam internal constructor (internal val value: Sequence<String>) : ParamValue {
    override fun asString(): String = value.joinToString(", ")
    override val values: Sequence<String> get() = value
  }

  /** Factories for obtaining a [ParamValue] instance. */
  public companion object {
    /** @return Wrapped [value] as a [ParamValue]. */
    @JvmStatic public fun of(value: String): ParamValue = StringParam(value)

    /** @return Param value or `null` if no value is available. */
    @JvmStatic public fun ofNullable(value: String?): ParamValue? = value?.let { StringParam(it) }

    /** @return Param value or `null` if no values are available. */
    @JvmStatic public fun ofNullable(values: Sequence<String>): ParamValue? = values.toList().let {
      when {
        it.isEmpty() -> null
        it.size == 1 -> StringParam(it.first())
        else -> MultiParam(it.asSequence())
      }
    }
  }
}
