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
 * ## HTTP Header
 *
 * Represents a single HTTP header, which consists of a [name] and [value]. HTTP headers are key-value pairs sent with
 * HTTP requests and responses, providing additional context or metadata about the request or response; most HTTP header
 * names are case-insensitive and standardized as part of the HTTP specification.
 */
public sealed interface Header: HttpToken {
  /**
   * ### Header Name
   *
   * Name, or key, portion of the HTTP header pair.
   */
  public val name: HeaderName

  /**
   * ### Header Value
   *
   * Value portion of the HTTP header pair.
   */
  public val value: HeaderValue

  // Render as an HTTP header pair.
  override fun asString(): String = "${name.asString()}: ${value.asString()}"

  /**
   * Raw header which wraps two strings: a header name and a header value.
   */
  @JvmInline public value class RawHeader internal constructor(
    private val pair: Pair<HttpHeaderValue, HttpHeaderValue>,
  ): Header {
    override val name: HeaderName get() = HeaderName.of(pair.first)
    override val value: HeaderValue get() = HeaderValue.single(pair.second)
  }

  /**
   * Typed header which wraps a [HeaderName] and a [HeaderValue].
   */
  @JvmInline public value class TypedHeader internal constructor(
    private val pair: Pair<HeaderName, HeaderValue>,
  ): Header {
    override val name: HeaderName get() = pair.first
    override val value: HeaderValue get() = pair.second
  }

  /**
   * Platform-specific extension point for implementing a header name/value pair.
   */
  public interface PlatformHeader: Header

  /** Factory for obtaining or creating [Header] values. */
  public companion object {
    /**
     * Create a new HTTP header name/value pair with the provided raw [name] and raw [value].
     *
     * @param name Raw header name
     * @param value Raw header value
     * @return Header pair instance
     */
    @JvmStatic public fun of(name: HttpHeaderName, value: HttpHeaderValue): Header =
      RawHeader(name to value)

    /**
     * Create a new HTTP header name/value pair with the provided raw [name] and typed [value].
     *
     * @param name Raw header name
     * @param value Typed header value
     * @return Header pair instance
     */
    @JvmStatic public fun of(name: HttpHeaderName, value: HeaderValue): Header =
      TypedHeader(HeaderName.of(name) to value)

    /**
     * Create a new HTTP header name/value pair with the provided [name] and [value].
     *
     * @param name Typed header name
     * @param value Typed header value
     * @return Header pair instance
     */
    @JvmStatic public fun of(name: HeaderName, value: HeaderValue): Header =
      TypedHeader(name to value)
  }
}
