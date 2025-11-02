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
 * ## HTTP Header Value
 *
 * Represents a single HTTP header value, which holds one or more [HttpHeaderValue] instances.
 */
public sealed interface HeaderValue {
  /**
   * Count of header values attached present.
   */
  public val count: UShort

  /**
   * Return the string value for this header; if this is a multi-value header, the string may be forcibly joined.
   */
  public fun asString(): HttpHeaderValue

  /**
   * Return the string value for this header; if this is a multi-value header, multiple strings are emitted.
   */
  public val values: Sequence<HttpHeaderValue>

  /**
   * Single header value, held as a string.
   *
   * Holds a single value ([HttpHeaderValue]).
   */
  @JvmInline public value class SingleHeaderValue internal constructor (
    internal val single: HttpHeaderValue,
  ) : HeaderValue {
    override val count: UShort get() = 1u
    override val values: Sequence<HttpHeaderValue> get() = sequenceOf(single)
    override fun asString(): HttpHeaderValue = single
    override fun toString(): String = single
  }

  /**
   * Multi header value.
   *
   * Holds a multiple values ([Sequence] of [HttpHeaderValue]).
   */
  @JvmRecord public data class MultiHeaderValue internal constructor (
    internal val value: Pair<UShort, Sequence<HttpHeaderValue>>,
  ) : HeaderValue {
    override val count: UShort get() = value.first
    override val values: Sequence<HttpHeaderValue> get() = value.second
    override fun asString(): HttpHeaderValue = value.second.joinToString(", ")
    override fun toString(): String = asString()
  }

  /** Factories for obtaining [HeaderValue] instances. */
  public companion object {
    /** @return Single header value as a string. */
    @JvmStatic public fun single(value: String): HeaderValue = SingleHeaderValue(value)

    /** @return Multi-header value holding the provided strings. */
    @JvmStatic public fun multi(collection: Collection<String>): HeaderValue =
      MultiHeaderValue(value = collection.size.toUShort() to collection.asSequence())

    /** @return Multi-header value holding the arguments. */
    @JvmStatic public fun multi(vararg strings: String): HeaderValue =
      MultiHeaderValue(value = strings.size.toUShort() to strings.asSequence())

    /** @return Multi-header value holding the provided sequence. */
    @JvmStatic public fun multi(seq: Sequence<String>): HeaderValue =
      MultiHeaderValue(value = seq.count().toUShort() to seq)
  }
}
