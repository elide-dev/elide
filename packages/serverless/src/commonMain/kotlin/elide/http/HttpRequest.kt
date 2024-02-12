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

import kotlin.jvm.JvmStatic
import elide.http.api.HttpMapping
import elide.http.api.HttpMethod
import elide.http.api.HttpRequest
import elide.http.api.HttpString
import elide.net.api.URL
import elide.http.api.HttpRequest as HttpRequestAPI

/**
 * # HTTP Request
 *
 * Cross-platform pure-Kotlin implementation of an HTTP request, including compliance with Elide's [HttpRequestAPI]; the
 * implementation is immutable by default and is expected to be thread-safe on all platforms (unless otherwise noted).
 *
 * &nbsp;
 *
 * ## Features
 *
 * TBD.
 *
 * ### Mutability
 *
 * TBD.
 *
 * ### Behavior
 *
 * TBD.
 *
 * ## Request Properties
 *
 * ### Method ([method])
 *
 * ### URL ([url])
 *
 * ### Path ([path])
 *
 * @see HttpMessage for fields and methods shared with HTTP messages.
 * @see HttpResponse for the corresponding implementation provided for HTTP responses.
 * @see HttpRequestAPI for the API supported by this implementation.
 */
public expect class HttpRequest private constructor() : HttpMessage, HttpRequestAPI {
  // HTTP method.
  override val method: HttpMethod

  // Calculated current URL.
  override val url: URL

  // Request path.
  override val path: HttpString

  // Query parameters.
  override val query: HttpMapping<HttpString, HttpString>

  /** Immutable HTTP request factory methods. */
  public companion object : HttpRequestAPI.Factory {
    // Create a new HTTP request with the given method, path, and query parameters.
    override fun create(
      method: HttpMethod,
      path: HttpString,
      query: HttpMapping<HttpString, HttpString>?
    ): HttpRequest

    // Copy an existing HTTP request with the given method, path, and query parameters.
    override fun copy(
      source: HttpRequest,
      method: HttpMethod?,
      path: HttpString?,
      query: HttpMapping<HttpString, HttpString>?
    ): HttpRequest
  }
}
