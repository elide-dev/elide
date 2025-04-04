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

import elide.core.api.Symbolic

/**
 * ## HTTP Method
 *
 * Describes the concept of an HTTP request method; methods indicate the requested operation type for a given HTTP
 * request, and typically govern how an HTTP server handles the request. Some methods imply (or require) a request body,
 * while others forbid a body or carry none by default.
 *
 * This interface defines the minimum viable properties that a method descriptor implementation must provide. This
 * includes the string name of the method, which is the token the server typically uses to identify the method.
 *
 * Some HTTP methods are standardized as part of HTTP itself; these are modeled with enumeration classes which
 * ultimately implement this interface. Custom methods can also be used over-the-wire, and these are modeled as wrapper
 * types over strings.
 *
 * @property symbol HTTP method name
 * @property permitsRequestBody Whether this method allows a request body
 * @property permitsResponseBody Whether this method allows a response body
 * @property requiresRequestBody Whether this method requires a request body
 */
public sealed interface Method: HttpToken, Symbolic<String> {
  /**
   * HTTP method name.
   *
   * This is the canonical name of the HTTP method, e.g. `GET`, `POST`, etc.
   */
  override val symbol: String

  /**
   * Whether this HTTP method permits a request body.
   */
  public val permitsRequestBody: Boolean

  /**
   * Whether this HTTP method permits a response body.
   */
  public val permitsResponseBody: Boolean

  /**
   * Custom HTTP method.
   *
   * Describes a custom HTTP method which is not part of the standard HTTP method set. This is typically used for server
   * interfaces which leverage custom HTTP methods as verbs.
   */
  @JvmInline public value class CustomMethod internal constructor(override val symbol: String) : Method {
    override val permitsRequestBody: Boolean get() = true
    override val permitsResponseBody: Boolean get() = true
    override val requiresRequestBody: Boolean get() = false
  }

  /**
   * Whether this HTTP method requires a request body.
   *
   * This is `true` for methods like `POST` and `PUT`, which require a request body to be sent.
   */
  public val requiresRequestBody: Boolean

  override fun asString(): String = symbol

  /**
   * Platform request: Extension point for platform-specific HTTP method implementations.
   */
  public interface PlatformMethod : Method

  /** Factories for creating or resolving HTTP methods. */
  public companion object {
    /** @return [Method] which is either custom or standard (if recognized). */
    @JvmStatic public fun of(symbol: String): Method = runCatching {
      StandardMethod.Companion.resolve(symbol)
    }.getOrElse {
      CustomMethod(symbol)
    }
  }
}
