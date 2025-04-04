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
 * ## HTTP Headers (Mutable)
 */
public sealed interface MutableHeaders: Headers {
  /**
   * Set a raw header value; note that this will clobber existing header values, rather than appending another value.
   * Use the [append] function to guarantee that other values will not be clobbered.
   *
   * @param key Header name to set
   * @param value Header value to set
   */
  public operator fun set(key: HttpHeaderName, value: HttpHeaderValue)

  /**
   * Set a typed header value; this overwrites any previous value, but is aware of multi-values as well as single-value
   * headers.
   *
   * To guarantee that other values are preserved, use the [append] function instead.
   *
   * @param key Header to set
   * @param value Header value to set
   */
  public operator fun set(key: HeaderName, value: HeaderValue)

  /**
   * Append a header at the provided [key] with the provided [value]; if this is the first value for this header, it is
   * expressed as a single header. If there is one or more existing values for this header, it is expressed as a
   * multi-value header.
   *
   * @param key Header name to append
   * @param value Header value to append
   */
  public fun append(key: HttpHeaderName, value: HttpHeaderValue)

  /**
   * Set a typed header at the provided [pair]; is an existing header is present at the same key, it is replaced with
   * the value held by the pair.
   *
   * @param pair Header to append
   */
  public fun set(pair: Header)

  /**
   * Append a typed header at the provided [pair]; if this is the first value for this header, it is expressed as a
   * single header. If there is one or more existing values for this header, it is expressed as a multi-value header.
   *
   * @param pair Header to append
   */
  public fun append(pair: Header)

  /**
   * Remove all header values present for the provided [name].
   *
   * @param name Header name for which to remove all values
   */
  public fun remove(name: HttpHeaderName)

  /**
   * Remove all header values present for the provided [header].
   *
   * @param header Header to remove values for
   */
  public fun remove(header: HeaderName)

  /**
   * Remove the header value matching the provided [pair]; this removes 0 or more values from the header, and preserves
   * other values.
   *
   * @param pair Header value pair to remove
   */
  public fun remove(pair: Header)

  /**
   * Remove the header value matching the provided [key] and [value]; this removes 0 or more values from the header, and
   * preserves other values.
   *
   * @param key Header name to remove
   * @param value Header value to remove
   */
  public fun remove(key: HttpHeaderName, value: HttpHeaderValue)

  /**
   * Remove the header value matching the provided [key] and [value]; this removes 0 or more values from the header, and
   * preserves other values.
   *
   * @param key Header name to remove
   * @param value Header value to remove
   */
  public fun remove(key: HeaderName, value: HeaderValue)

  // Return self (already mutable).
  override fun toMutable(): MutableHeaders = this

  /**
   * Build these mutable headers into an immutable set of headers.
   *
   * @return Immutable headers
   */
  public fun build(): Headers

  /**
   * Platform-specific extension type for mutable headers.
   */
  public interface PlatformMutableHeaders: MutableHeaders
}
