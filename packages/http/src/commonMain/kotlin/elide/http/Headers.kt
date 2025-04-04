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
 * ## HTTP Headers
 *
 * Represents a container of HTTP [Header] values; the container behaves as an order-aware collection, which is capable
 * of storing multi-values for a given header name. Optimized access is provided in unordered form and for quick contain
 * matching and retrieval.
 *
 * HTTP headers are present on both requests and responses. This type is shared across both message types, and is made
 * available in a mutable form ([MutableHeaders]) for mutable requests and responses.
 *
 * ### Expression of HTTP Headers
 *
 * Headers *almost* behave like a map of strings to strings. There are a few corner cases to handle:
 *
 * - Headers can express more than one value per key, and this is common in practice. For example, the `Set-Cookie`
 *   response header may be emitted more than once to set more than one cookie. Type hierarchy is provided to solve this
 *   problem in Elide: a [HeaderValue] instance can be single or repeated.
 *
 * - Headers often follow standard conventions: header keys defined as part of the HTTP specification can be expressed
 *   in typed form, as [HeaderName] instances. These instances are normalized, checked, and self-describing.
 *
 * - Headers are case-insensitive, and the HTTP specification does not require a specific case for header keys. This
 *   structure will preserve case but makes keys available in normalized form for convenience.
 *
 * - Headers are order-aware, and the order of insertion is preserved. Most applications don't depend on header order,
 *   but some do, so Elide supports it. Use [asOrdered] to obtain an ordered collection of headers.
 */
public sealed interface Headers {
  /**
   * Indicate whether any header values are present at the given key; the key will not be normalized.
   *
   * `true` is returned if at least one value is present for the provided header; if this method returns truthy, it is
   * guaranteed that a non-null value is available (at least one) at the provided key.
   *
   * @param name Name of the header to check
   * @return `true` if the header is present, `false` otherwise
   */
  public operator fun contains(name: HttpHeaderName): Boolean

  /**
   * Indicate whether any header values are present at the given [header]; the key is already normalized in this case.
   *
   * `true` is returned if at least one value is present for the provided header; if this method returns truthy, it is
   * guaranteed that a non-null value is available (at least one) at the provided key.
   *
   * @param header Header key to check
   * @return `true` if the header is present, `false` otherwise
   */
  public operator fun contains(header: HeaderName): Boolean

  /**
   * Retrieve a header value by string key; the header will not be normalized.
   *
   * If no header value is present at all, `null` is returned; if one or more values are present, the values are
   * provided either in a single-value or multi-value form.
   *
   * @param name Header name as a string
   * @return Single or multi-header value
   */
  public operator fun get(name: HttpHeaderName): HeaderValue?

  /**
   * Retrieve a header value by known key; the header is normalized in this form.
   *
   * If no header value is present at all, `null` is returned; if one or more values are present, the values are
   * provided either in a single-value or multi-value form.
   *
   * @param header Header key
   * @return Single or multi-header value
   */
  public operator fun get(header: HeaderName): HeaderValue?

  /**
   * Obtain a sequence of all present headers which preserves order.
   *
   * @return Ordered sequence of headers
   */
  public fun asOrdered(): Sequence<Header>

  /**
   * Obtain a sequence of all headers with no guarantees about order.
   *
   * @return Sequence of headers.
   * @see asOrdered Ordered sequence of headers
   */
  public fun asSequence(): Sequence<Header>

  /**
   * Obtain a map of all headers present within this headers container, including their multiple values, if present.
   *
   * This method provides a map of typed header names (custom or standard) to their header values (single or multi).
   *
   * @return Map of headers to their values
   */
  public fun asMap(): Map<HeaderName, HeaderValue>

  /**
   * Obtain a map of all headers present within this headers container, including their multiple values, if present.
   *
   * This method provides a list of string values for each present header. Header names are not transformed, so they are
   * expressed as they were inserted.
   *
   * @return Map of headers to their values
   */
  public fun asRawMap(): Map<HttpHeaderName, List<HttpHeaderValue>>

  /**
   * Retrieve the first header value present for the provided [name]; if no value is present at all, return `null`.
   *
   * This method will normalize keys for matching.
   *
   * @return First header value, or `null` if no value is present
   */
  public fun first(name: HttpHeaderName): HttpHeaderValue?

  /**
   * Retrieve the first value present for the provided [header]; if no value is present at all, return `null`.
   *
   * @return First header value, or `null` if no value is present
   */
  public fun first(header: HeaderName): HttpHeaderValue?

  /**
   * Build these immutable headers into a mutable set of headers; if these headers are already mutable, this may return
   * the same object.
   *
   * @return Mutable headers
   */
  public fun toMutable(): MutableHeaders

  /**
   * Count of all headers present within this headers container, regardless of uniqueness.
   *
   * This property is typically known ahead-of-time.
   */
  public val size: UInt

  /**
   * Count of all distinct headers present within this headers container.
   *
   * This property is typically known ahead-of-time.
   */
  public val sizeDistinct: UInt

  /**
   * Extension point for platform-specific HTTP headers implementations.
   */
  public interface PlatformHeaders: Headers
}
