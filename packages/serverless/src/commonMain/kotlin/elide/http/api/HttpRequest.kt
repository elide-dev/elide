/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import elide.http.api.HttpMessageType.REQUEST
import elide.net.api.URL

/**
 * # HTTP: Request
 *
 * Describes the API provided for HTTP requests; request messages extend [HttpMessage] with request-specific information
 * and behavior.
 *
 * ## Method ([method])
 *
 * Describes the HTTP method for the request expressed by this record. Methods are either standard HTTP methods, or
 * customized HTTP methods for exotic use cases.
 *
 * ## Path ([path])
 *
 * Request path which was enclosed within this HTTP request.
 *
 * ## URL ([url])
 *
 * Calculated full URL, as derived from the request path, query parameters, and server configuration.
 *
 * ## Query ([query])
 *
 * Describes the query parameters for the request, present because they were parsed from the request URL.
 *
 * @see HttpResponse for the response counterpart to this interface.
 * @see MutableHttpRequest for the mutable form of this interface.
 */
@API public interface HttpRequest : HttpMessage {
  /**
   * # HTTP Request: Defaults
   *
   * Default values used in newly-created HTTP requests, unless overridden.
   */
  public data object Defaults {
    /** Default HTTP method for new requests. */
    public val METHOD: HttpMethod = StandardHttpMethod.GET

    /** Default request path for new requests. */
    public const val PATH: HttpString = "/"
  }

  /**
   * # HTTP Request: Factory
   *
   * Describes the API provided for creating new HTTP requests; this is typically used to lay out the static method
   * structure on a HTTP request implementation.
   *
   * &nbsp;
   *
   * ## Mutability
   *
   * Implementations of this interface are expected to be immutable, and should not allow modification of the request
   * method, path, or query parameters after creation (or any other properties). For a mutable form of this interface,
   * see [MutableHttpRequest.Factory].
   *
   * ## Request Behavior
   *
   * See the main [HttpRequest] interface for more information about how HTTP requests behave under the hood.
   *
   * @see HttpRequest for the immutable HTTP request API this factory creates.
   * @see MutableHttpRequest.Factory for the mutable form of this interface.
   */
  @API public interface Factory {
    /**
     * Creates a new HTTP request with the given method, path, and query parameters.
     *
     * @param method HTTP method for the request.
     * @param path Request path for the request.
     * @param query Query parameters for the request.
     * @return A new HTTP request with the given parameters.
     */
    public fun create(
      method: HttpMethod = Defaults.METHOD,
      path: HttpString = Defaults.PATH,
      query: HttpMapping<HttpString, HttpString>? = null,
    ): HttpRequest

    /**
     * Copies an existing HTTP request, with the option to override the method, path, and query parameters.
     *
     * @param source HTTP request to copy.
     * @param method HTTP method for the request.
     * @param path Request path for the request.
     * @param query Query parameters for the request.
     * @return A new HTTP request with the given parameters.
     */
    public fun copy(
      source: HttpRequest,
      method: HttpMethod? = null,
      path: HttpString? = null,
      query: HttpMapping<HttpString, HttpString>? = null,
    ): HttpRequest
  }

  // Always `REQUEST`.
  override val type: HttpMessageType get() = REQUEST

  // Always `false`.
  override val mutable: Boolean get() = false

  /**
   * Describes the HTTP method for the request expressed by this record. Methods are either standard HTTP methods, or
   * customized HTTP methods for exotic use cases.
   */
  public val method: HttpMethod

  /**
   * Request path which was enclosed within this HTTP request.
   */
  public val path: HttpString

  /**
   * Calculated full URL, as derived from the request path, query parameters, and server configuration.
   */
  public val url: URL

  /**
   * Describes the query parameters for the request, present because they were parsed from the request URL.
   */
  public val query: HttpMapping<HttpString, HttpString>
}
