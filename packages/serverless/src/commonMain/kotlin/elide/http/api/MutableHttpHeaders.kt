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

import elide.annotations.API
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpHeaders.HeaderValue

/**
 * # HTTP Headers (Mutable)
 *
 * Describes the API provided for mutable mapping structures specialized to store HTTP headers associated with values;
 * behaves as a sorted multi-map with multi-values behaving as single values unless addressed with iterators.
 *
 * [HeaderName] keys are normalized for case and trailing/leading whitespace; order of insertion is preserved for values
 * via [HeaderValue].
 *
 * &nbsp;
 *
 * ## Mutability
 *
 * This structure describes the mutable form of [HttpHeaders]. Headers can be added and changed after creation in an
 * arbitrary manner. The underlying structure should preserve insertion order for values associated with a given key,
 * and should produce sorted keys when iterated or exported.
 *
 * &nsbp;
 *
 * ## Behavior
 *
 * Aside from mutability methods like [set], [put], [putAll], [remove], [clear], and so on, this structure behaves as a
 * regular [HttpHeaders] mapping. It is recommended to use the [HttpHeaders] interface when the structure is not being
 * modified.
 *
 * For information about how HTTP header mappings behave, see [HttpHeaders].
 *
 * @see HttpHeaders for the immutable form of this structure.
 */
@API public interface MutableHttpHeaders : HttpHeaders, MutableHttpMapping<HeaderName, HeaderValue> {
  /**
   * ## Mutable HTTP Headers: Factory
   *
   * Factory methods for creating instances of [MutableHttpHeaders]; extends the base [HttpHeaders.Factory].
   */
  @API public interface Factory : HttpHeaders.Factory {
    /**
     * Create an empty mutable HTTP headers mapping.
     *
     * @return Empty mutable HTTP headers mapping.
     */
    public fun create(): MutableHttpHeaders

    /**
     * Create a mutable HTTP headers mapping from a list of header name-value pairs.
     *
     * @param pairs List of header name-value pairs to create the mapping from.
     * @return Mutable HTTP headers mapping from the list of header name-value pairs.
     */
    override fun of(vararg pairs: Pair<String, String>): MutableHttpHeaders

    /**
     * Create a mutable HTTP headers mapping from a map of header name-value pairs.
     *
     * @param map Map of header name-value pairs to create the mapping from.
     * @return Mutable HTTP headers mapping from the map of header name-value pairs.
     */
    override fun of(map: Map<String, String>): MutableHttpHeaders

    /**
     * Create a mutable HTTP headers mapping from a collection of header name-value pairs.
     *
     * @param collection Collection of header name-value pairs to create the mapping from.
     * @return Mutable HTTP headers mapping from the collection of header name-value pairs.
     */
    override fun of(collection: Collection<Pair<String, String>>): MutableHttpHeaders

    /**
     * Create a mutable HTTP headers mapping from a sequence of header name-value pairs.
     *
     * @param pairs Sequence of header name-value pairs to create the mapping from.
     * @return Mutable HTTP headers mapping from the sequence of header name-value pairs.
     */
    override fun of(pairs: Sequence<Pair<String, String>>): MutableHttpHeaders
  }

  /**
   * Set a [value] for a given header [key].
   *
   * This operator function handles assignments to the header mapping, as [HeaderName] keys. In the case of `set` calls
   * like this one, the header multi-value mapping is overwritten (as applicable).
   *
   * @see add for adding a value to a header without overwriting existing values.
   * @param key Header name to set.
   * @param value Value to set for the header.
   */
  public operator fun set(key: HeaderName, value: HttpString)

  /**
   * Set a [value] for a given header [key] string.
   *
   * This operator function handles assignments to the header mapping, as [String] keys. In the case of `set` calls like
   * this one, the header multi-value mapping is overwritten (as applicable).
   *
   * @see add for adding a value to a header without overwriting existing values.
   * @param key Header name to set.
   * @param value Value to set for the header.
   */
  public operator fun set(key: String, value: HttpString) {
    set(HeaderName.of(key), value)
  }

  /**
   * Add a [value] to a given header [key].
   *
   * This operator function handles adding a value to the header mapping, as [HeaderName] keys. In the case of `add`
   * calls like this one, the header multi-value mapping is appended to (as applicable).
   *
   * Single-value headers are converted to multi-value headers as needed.
   *
   * @see set for setting a value for a header, overwriting existing values.
   * @param key Header name to add to.
   * @param value Value to add to the header.
   */
  public fun add(key: HeaderName, value: HttpString)

  /**
   * Add a [value] to a given header [key] string.
   *
   * This operator function handles adding a value to the header mapping, as [String] keys. In the case of `add` calls
   * like this one, the header multi-value mapping is appended to (as applicable).
   *
   * Single-value headers are converted to multi-value headers as needed.
   *
   * @see set for setting a value for a header, overwriting existing values.
   * @param key Header name to add to.
   * @param value Value to add to the header.
   */
  public fun add(key: String, value: HttpString) {
    add(HeaderName.of(key), value)
  }

  /**
   * Remove a [value] to a given header [key], if present.
   *
   * This operator function handles removing a value to the header mapping, with a [HeaderName] key. Multi-value headers
   * are converted to single-value headers as needed. If no matching header is present, this method is a no-op.
   *
   * @see add for adding a value to a header, preserving existing values.
   * @param key Header name to remove from.
   * @param value Value to remove from the header.
   */
  public fun remove(key: HeaderName, value: HttpString)

  /**
   * Remove all values for a given header [key], if present.
   *
   * If no matching header is present, this method is a no-op.
   *
   * @see add for adding a value to a header, preserving existing values.
   * @param key Header value that was removed, as applicable.
   */
  override fun remove(key: HeaderName): HeaderValue?
}
