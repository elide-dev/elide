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
 * ## HTTP Header Name
 *
 * Represents the name, or key, portion, of an HTTP header value; headers are typically standard fields defined by a
 * version of the HTTP specification, but can also be custom string values defined by the user or application.
 */
public sealed interface HeaderName: HttpToken {
  /**
   * Name of the HTTP header.
   */
  public val name: HttpHeaderName

  /**
   * Normalized name of the HTTP header.
   */
  public val nameNormalized: HttpHeaderName

  /**
   * Whether this header is present/allowed on requests.
   */
  public val allowedOnRequests: Boolean

  /**
   * Whether this header is present/allowed on responses.
   */
  public val allowedOnResponses: Boolean

  /**
   * ### String Header Name
   *
   * Represents a custom HTTP header name.
   */
  @JvmInline public value class StringHeaderName(
    override val name: HttpHeaderName,
  ) : HeaderName {
    override val allowedOnRequests: Boolean get() = true
    override val allowedOnResponses: Boolean get() = true
    override val nameNormalized: HttpHeaderName get() = name.lowercase().trim()
    override fun asString(): String = nameNormalized
  }

  /**
   * ### Standard Header Name
   *
   * Represents a known standard HTTP header name.
   */
  @JvmInline public value class StdHeaderName(internal val std: StandardHeader) : HeaderName {
    override val name: HttpHeaderName get() = std.symbol
    override val nameNormalized: HttpHeaderName get() = name
    override val allowedOnRequests: Boolean get() = std.allowedOnRequests
    override val allowedOnResponses: Boolean get() = std.allowedOnResponses
    override fun asString(): String = nameNormalized
  }

  /**
   * Platform-specific header name extension point.
   */
  public interface PlatformHeaderName: HeaderName

  /** Utilities for creating or resolving header names. */
  public companion object {
    /** @return String HTTP header name. */
    @JvmStatic public fun of(name: String): HeaderName = StringHeaderName(name)

    /** @return Standard HTTP header name. */
    @JvmStatic public fun of(std: StandardHeader): HeaderName = StdHeaderName(std)
  }
}
