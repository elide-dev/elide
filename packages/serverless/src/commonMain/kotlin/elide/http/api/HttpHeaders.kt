/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package elide.http.api

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import elide.annotations.API
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpHeaders.HeaderValue

/**
 * # HTTP Headers
 *
 * Describes the API provided for mapping structures specialized to store HTTP headers associated with values; behaves
 * as a sorted multi-map with multi-values behaving as single values unless addressed with iterators.
 *
 * [HeaderName] keys are normalized for case and trailing/leading whitespace; order of insertion is preserved for values
 * via [HeaderValue].
 *
 * &nbsp;
 *
 * ## Mutability
 *
 * This structure is immutable by default; to build a mutable HTTP header container, see [MutableHttpHeaders].
 * The nested [Factory] interface describes companion methods for creating new maps statically, including a convenient
 * shortcut (`mutable`) to create a [MutableHttpHeaders] instance directly from [HttpHeaders].
 *
 * &nbsp;
 *
 * ## Behavior
 *
 * This structure behaves as a multi-map, allowing multiple values for a single header name. When accessed as a map, the
 * first value is returned; to access all values for a header, use the [getAll] method, or obtain an iterator.
 *
 * Iteration is different for keys versus values held by this structure: keys are iterated in sorted order, while values
 * are order-preserved.
 *
 * Implementations of this interface must remain compliant with several specifications and APIs at once, including: (1)
 * HTTP APIs provided by the JDK and by Netty, (2) standard interfaces which are part of the JDK, and (3) external HTTP
 * specifications like WhatWG Fetch.
 *
 * &nbsp;
 *
 * ## Internal Types
 *
 * This structure uses [HeaderName] and [HeaderValue] to normalize and store header names and values, respectively. Each
 * type extends basic types like [CharSequence] and [Comparable] to allow for easy access and comparison. HTTP header
 * containers are themselves comparable on this basis.
 *
 * ## Usage
 *
 * The standard implementations of these types are held in the package `elide.http`; so, for example:
 *
 * ```kotlin
 * import elide.http.HttpHeaders
 *
 * // immutable headers:
 * val headers = HttpHeaders.of("Content-Type" to "application/json")
 * assert("Content-Type" in headers)
 * assert("Content-Type" in headers)
 * assert("application/json" == headers["Content-Type"])
 * assert("application/json" == headers[HeaderName.of("Content-Type")])
 *
 * // mutable headers:
 * val mutable = MutableHttpHeaders.of("Content-Type" to "application/json")
 * mutable["Content-Type"] = "application/xml"
 * assert("application/xml" == mutable["Content-Type"])
 * ```
 *
 * @see MutableHttpHeaders For the interface provided for mutable HTTP header containers, which extends this one.
 * @see elide.http.HttpHeaders For the default implementation of an immutable HTTP header mapping.
 * @see elide.http.MutableHttpHeaders For the default implementation of a mutable HTTP header mapping.
 */
@API public interface HttpHeaders : HttpMapping<HeaderName, HeaderValue> {
  /**
   * ## HTTP Headers: Factory
   *
   * Describes static factory methods which create immutable HTTP header mappings; see [HttpHeaders] for more info about
   * how headers are stored and accessed.
   *
   * Mutable HTTP header containers are accessible through the corresponding factory interface on that type, at
   * [MutableHttpHeaders.Factory]. That factory extends this one.
   *
   * @see MutableHttpHeaders.Factory to create mutable HTTP headers.
   */
  @API public interface Factory {
    /**
     * Create an immutable container of HTTP headers from a list of key-value pairs.
     *
     * The resulting container cannot be mutated. To create a mutable container from a list of key-value pairs, see
     * [MutableHttpHeaders.Factory.of].
     *
     * @see MutableHttpHeaders.Factory.of to create a mutable headers mapping from pairs.
     * @param pairs A list of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun of(vararg pairs: Pair<String, String>): HttpHeaders

    /**
     * Create an immutable container of HTTP headers from a list of key-value pairs.
     *
     * The resulting container cannot be mutated. To create a mutable container from a list of key-value pairs, see
     * [MutableHttpHeaders.Factory.of].
     *
     * @see MutableHttpHeaders.Factory.of to create a mutable headers mapping from pairs.
     * @param collection A collection of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun of(collection: Collection<Pair<String, String>>): HttpHeaders

    /**
     * Create an immutable container of HTTP headers from a map of key-value pairs.
     *
     * The resulting container cannot be mutated. To create a mutable container from a map of key-value pairs, see
     * [MutableHttpHeaders.Factory.of].
     *
     * @see MutableHttpHeaders.Factory.of to create a mutable headers mapping from a map.
     * @param map A map of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the map of key-value pairs.
     */
    public fun of(map: Map<String, String>): HttpHeaders

    /**
     * Create an immutable container of HTTP headers from a sequence of key-value pairs.
     *
     * The resulting container cannot be mutated. To create a mutable container from a list of key-value pairs, see
     * [MutableHttpHeaders.Factory.of].
     *
     * @see MutableHttpHeaders.Factory.of to create a mutable headers mapping from pairs.
     * @param pairs A sequence of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun of(pairs: Sequence<Pair<String, String>>): HttpHeaders
  }

  /**
   * ## HTTP: Header Name
   *
   * Normalized HTTP header name; always compared in lower-cased/whitespace-trimmed form. Accessible by string or other
   * (matching) [HeaderName] instances.
   *
   * &nbsp;
   *
   * ### Normalization
   *
   * Header names are always lower-cased and whitespace-trimmed for comparison. The original header value is held for
   * later access if needed.
   *
   * ### Factory methods
   *
   * This class has a private constructor to establish guarantees about the header name. To create a new header name,
   * use the [of] factory or the [asHeaderName] extension to [String].
   *
   * @param name The name of the HTTP header.
   * @param standard Whether this header is known to be standard; internal use only.
   */
  public class HeaderName private constructor (
    public val name: CaseInsensitiveHttpString,
    private val standard: Boolean = false,
  ) : CharSequence, Comparable<HeaderName> {
    public companion object {
      /**
       * Create a new header name from a string.
       *
       * @receiver The name of the header to create.
       * @return A new header name from the given string.
       */
      public val String.asHeaderName: HeaderName get() = of(this)

      /**
       * Create a new header name from a string.
       *
       * @param name The name of the header to create.
       * @return A new header name from the given string.
       */
      @JvmStatic public fun of(name: String): HeaderName = HeaderName(CaseInsensitiveHttpString.of(name))

      /**
       * Create a new header name from a string.
       *
       * @param name The name of the header to create.
       * @return A new header name from the given string.
       */
      @JvmStatic internal fun std(name: String): HeaderName = HeaderName(CaseInsensitiveHttpString.of(name), true)
    }

    override val length: Int get() = name.length
    override fun get(index: Int): Char = name[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = name.subSequence(startIndex, endIndex)

    override fun compareTo(other: HeaderName): Int {
      return name.hashCode().compareTo(other.name.hashCode())
    }

    override fun toString(): String = name.toString()

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      return when (other) {
        is HeaderName -> name == other.name
        is String -> name.equals(other.trim().lowercase())
        else -> false
      }
    }

    override fun hashCode(): Int = name.hashCode()
  }

  /**
   * # HTTP: Header Value
   *
   * Describes all valid cases for HTTP header values, including single and multi-value headers, and exotic header types
   * which are used from standard header definitions (for example, tokenized or encoded headers).
   *
   * &nbsp;
   *
   * ## Behavior
   *
   * **1) String behavior.**
   *
   * This type behaves as a [Comparable] and [HttpString] (a [CharSequence]), allowing for easy comparison and access to
   * the underlying string value. It also provides access to all values for a header, if there are multiple values. As
   * multiple header values under the same name is typically a rare case, implementations should optimize for a single
   * value (or `2` values, at most).
   *
   * **2) Iterable behavior.**
   *
   * When this type is iterated over, it behaves as a collection of strings, allowing for easy access to all values for
   * a given header name. Order must be preserved by implementations, as it is expressed during construction.
   *
   * **3) Collection behavior.**
   *
   * This type behaves as a [Collection] of [String], allowing for easy access to all values for a given header name.
   * Order must be preserved by implementations, as it is expressed during construction.
   */
  public sealed interface HeaderValue : Comparable<String>, Collection<String> {
    /** Access the singular header value. */
    public val asString: String

    /** Access all values for the header. */
    public val allValues: List<HttpString>

    /** Count of values for this header. */
    override val size: Int get() = allValues.size

    /**
     * Compare a header value to some [other] string.
     *
     * @param other The string to compare the header value to.
     * @return The result of comparing the header value to the provided string.
     */
    override fun compareTo(other: String): Int = asString.compareTo(other)

    /**
     * Compare a header value to another header value.
     *
     * @param other The header value to compare to.
     * @return The result of comparing the header value to the provided header value.
     */
    public fun compareTo(other: HeaderValue): Int = asString.compareTo(other.asString)

    override fun iterator(): Iterator<String> = allValues.iterator()
    override fun contains(element: String): Boolean = allValues.contains(element)
    override fun containsAll(elements: Collection<String>): Boolean = elements.all { allValues.contains(it) }
    override fun isEmpty(): Boolean = false  // always has a value

    /** Static methods for creating HTTP header values. */
    public companion object {
      /**
       * Create a header value from the provided collection of string values.
       *
       * If the provided collection only has one value, a [SingleValue] header is created; otherwise, a [MultiValue]
       * header is created.
       *
       * @param value The collection of string values to create the header from.
       * @return A new header value from the provided collection of string values.
       */
      @JvmStatic public fun of(value: Collection<String>): HeaderValue =
        if (value.size > 1) MultiValue(value.toTypedArray()) else SingleValue(value.first())

      /**
       * Create a singular HTTP header value.
       *
       * @param value The value of the header to create.
       * @return A new singular HTTP header value.
       */
      @JvmStatic public fun single(value: String): HeaderValue = SingleValue(value)

      /**
       * Create a multi-value HTTP header value from a collection of values.
       *
       * @param values The values of the header to create.
       * @return A new multi-value HTTP header value.
       */
      @JvmStatic public fun multi(values: Collection<String>): HeaderValue = MultiValue(values.toTypedArray())

      /**
       * Create a multi-value HTTP header value from the provided parameters.
       *
       * @param values The values of the header to create.
       * @return A new multi-value HTTP header value.
       */
      @JvmStatic public fun multi(vararg values: String): HeaderValue = MultiValue(values)
    }

    /**
     * ## HTTP: Singular Header Value
     *
     * Describes a single HTTP header value, expressed as a simple [String]; the [value] is preserved as original for
     * the entire lifecycle of the object.
     *
     * @param value Header value.
     */
    @JvmInline
    public value class SingleValue(private val value: String) : HeaderValue, Comparable<String> {
      override val asString: String get() = value
      override val allValues: List<HttpString> get() = listOf(value)
      override val size: Int get() = 1
      override fun compareTo(other: String): Int = asString.compareTo(other)
      override fun toString(): String = value
      override fun iterator(): Iterator<String> = listOf(value).iterator()
      override fun contains(element: String): Boolean = value == element
      override fun containsAll(elements: Collection<String>): Boolean = elements.all { value == it }
      override fun isEmpty(): Boolean = false  // always has a value
    }

    /**
     * ## HTTP: Multi-Value Header Value
     *
     * Describes a multi-value HTTP header, expressed as a collection of [String] values; each value is preserved for
     * the lifecycle of the object.
     *
     * @param values Header values.
     */
    @JvmInline
    public value class MultiValue(private val values: Array<out String>) : HeaderValue, Comparable<String> {
      override fun toString(): String = asString
      override val asString: String get() = values.joinToString(DEFAULT_SEPARATOR)
      override val allValues: List<HttpString> get() = values.toList()

      internal fun add(value: String): MultiValue = MultiValue(
        Array(values.size + 1) { i -> if (i == values.size) value else values[i] }
      )

      internal fun remove(value: String): MultiValue = MultiValue(
        values.filter { it != value }.toTypedArray()
      )
    }
  }

  /**
   * Get a singular value for the header named by [key].
   *
   * If there are multiple values for the header (because of multiple header instances with the same name, or a header
   * value split from tokens), then the first value is returned. To access all values for a header, use [getAll] or
   * obtain an iterator.
   *
   * @see [get] to retrieve a header by string.
   * @see [getAll] to retrieve all values for a header.
   * @param key The name of the header to get the value for.
   * @return The singular value for the header named by [key]; if no matching header is present, `null` is returned.
   */
  public override operator fun get(key: HeaderName): HeaderValue?

  /**
   * Get a singular value for the header string named by [key].
   *
   * If there are multiple values for the header (because of multiple header instances with the same name, or a header
   * value split from tokens), then the first value is returned. To access all values for a header, use [getAll] or
   * obtain an iterator.
   *
   * @see [get] to retrieve a header by [HeaderName].
   * @see [getAll] to retrieve all values for a header.
   * @param key The name of the header to get the value for.
   * @return The singular value for the header named by [key]; if no matching header is present, `null` is returned.
   */
  public operator fun get(key: String): String? = get(HeaderName.of(key))?.asString

  /**
   * Get all values for the header named by [key].
   *
   * @see [get] to retrieve a singular value for a header.
   * @param key The name of the header to get all values for.
   * @return All values for the header named by [key] as a [Sequence] of values, with order preserved; if no matching
   *  header is present, an empty sequence is returned.
   */
  public fun getAll(key: HeaderName): Sequence<HttpString>

  /**
   * Get all values for the header named by [key].
   *
   * @see [get] to retrieve a singular value for a header.
   * @param key The name of the header to get all values for.
   * @return All values for the header named by [key] as a [Sequence] of values, with order preserved; if no matching
   *   header is present, an empty sequence is returned.
   */
  public fun getAll(key: String): Sequence<HttpString> = getAll(HeaderName.of(key))

  /**
   * Check if the header named by [key] is present.
   *
   * @see [contains] to check for a header by string.
   * @param key The name of the header to check for.
   * @return `true` if the header named by [key] is present; `false` otherwise.
   */
  public operator fun contains(key: HeaderName): Boolean

  /**
   * Check if the header string [key] is present.
   *
   * @see [contains] to check for a header by [HeaderName].
   * @param key The name of the header to check for.
   * @return `true` if the header named by [key] is present; `false` otherwise.
   */
  public operator fun contains(key: String): Boolean = contains(HeaderName.of(key))
}
