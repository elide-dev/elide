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

package elide.http

import elide.http.api.HttpHeaders as HttpHeadersAPI
import elide.http.api.MutableHttpHeaders as MutableHttpHeadersAPI
import kotlin.jvm.JvmStatic
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpHeaders.HeaderValue
import elide.http.api.HttpString
import elide.struct.MutableTreeMap
import elide.struct.api.SortedMap
import elide.struct.sortedMapOf

/**
 * # HTTP Headers
 *
 * Immutable container which maps HTTP headers to their values; supports case-insensitive and ordered comparison by
 * header key, and preserves insertion order for header values. See [HttpHeadersAPI] for more information.
 *
 * @see MutableHttpHeaders for the mutable counterpart to this class.
 * @see HttpHeadersAPI for the API definition of this class.
 * @see HttpHeadersAPI.Factory for the factory methods to create instances of this class.
 */
public class HttpHeaders private constructor (
  private val headers: SortedMap<HeaderName, HeaderValue> = sortedMapOf(),
) : HttpHeadersAPI, Map<HeaderName, HeaderValue> by headers {
  /** [HttpHeadersAPI.Factory] methods. */
  public companion object : HttpHeadersAPI.Factory {
    // Empty singleton used for immutable empty headers.
    private val EMPTY_SINGLETON = HttpHeaders()

    /**
     * Create an empty immutable container of HTTP headers.
     *
     * This value cannot be mutated later, so a sentinel map is used under the hood to avoid unnecessary allocations. To
     * create an empty mutable container, see [MutableHttpHeadersAPI.Factory.create].
     *
     * @see MutableHttpHeadersAPI.Factory.create to create an empty mutable headers mapping.
     * @return An empty immutable container of HTTP headers.
     */
    @JvmStatic public fun empty(): HttpHeaders = EMPTY_SINGLETON

    /**
     * Create a mutable container of HTTP headers from a list of key-value pairs.
     *
     * The resulting container can be mutated as needed. To create an immutable container from a list of key-value pairs
     * see [of].
     *
     * @param pairs A list of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun mutable(vararg pairs: Pair<String, String>): MutableHttpHeaders = mutable(pairs.asSequence())

    /**
     * Create a mutable container of HTTP headers from a collection of key-value pairs.
     *
     * The resulting container can be mutated as needed. To create an immutable container from a list of key-value pairs
     * see [of].
     *
     * @param collection A collection of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun mutable(collection: Collection<Pair<String, String>>): MutableHttpHeaders = mutable(
      collection.asSequence()
    )

    /**
     * Create a mutable container of HTTP headers from a sequence of key-value pairs.
     *
     * The resulting container can be mutated as needed. To create an immutable container from a list of key-value pairs
     * see [of].
     *
     * @param collection A collection of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun mutable(collection: Sequence<Pair<String, String>>): MutableHttpHeaders =
      MutableHttpHeaders.of(collection)

    /**
     * Create a mutable container of HTTP headers from a map of key-value pairs.
     *
     * The resulting container can be mutated as needed. To create an immutable container from a list of key-value pairs
     * see [of].
     *
     * @param map A map of key-value pairs to create the HTTP headers from.
     * @return An immutable container of HTTP headers from the list of key-value pairs.
     */
    public fun mutable(map: Map<String, String>): MutableHttpHeaders = MutableHttpHeaders.of(map)

    @JvmStatic override fun of(vararg pairs: Pair<String, String>): HttpHeaders = of(pairs.asSequence())

    @JvmStatic override fun of(collection: Collection<Pair<String, String>>): HttpHeaders = of(collection.asSequence())

    @JvmStatic override fun of(map: Map<String, String>): HttpHeaders = sortedMapOf(
      map.entries.map { HeaderName.of(it.key) to HeaderValue.single(it.value) }
    ).let {
      HttpHeaders(it)
    }

    @JvmStatic override fun of(pairs: Sequence<Pair<String, String>>): HttpHeaders = sortedMapOf(
      // map each pair into a `HeaderName` and string value, then group by `HeaderName`
      pairs.map { HeaderName.of(it.first) to it.second }.groupingBy {
        it.first
      }.foldTo(MutableTreeMap.create<HeaderName, ArrayList<String>>(), ArrayList(2)) { acc, el ->
        // fold into a collection of string values for each `HeaderName`, preserving insertion order
        acc.apply {
          add(el.second)
        }
      }.entries.asSequence().map {
        // convert each `HeaderName` and `LinkedHashSet` of string values into a `HeaderName` and single `HeaderValue`
        it.key to HeaderValue.of(it.value)
      }.toList()
    ).let {
      HttpHeaders(it)
    }
  }

  override operator fun get(key: HeaderName): HeaderValue? = headers[key]
  override operator fun contains(key: HeaderName): Boolean = headers.containsKey(key)
  override fun getAll(key: HeaderName): Sequence<HttpString> = headers[key]?.allValues?.asSequence() ?: emptySequence()
}
