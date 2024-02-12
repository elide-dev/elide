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
import elide.net.api.URL

/**
 * # HTTP Request (Mutable)
 */
@API public interface MutableHttpRequest : MutableHttpMessage, HttpRequest {
  /**
   * # Mutable HTTP Request: Factory
   *
   * Describes the API provided for creating new mutable HTTP requests; this interface is typically used to model the
   * available static methods on a companion object for an HTTP request implementation. Methods are expected to be
   * static where applicable.
   *
   * &nbsp;
   *
   * ## Mutability
   *
   * Implementations of this interface are expected to be mutable, and should allow open modification of the request
   * method, path, or query parameters after creation (or any other properties). For an immutable form of this interface
   * see [HttpRequest.Factory].
   *
   * &nsbp;
   *
   * ## Request Behavior
   *
   * See the main [HttpRequest] interface for more information about how HTTP requests behave under the hood.
   *
   * @see MutableHttpRequest for the mutable HTTP request API this factory creates.
   * @see HttpRequest.Factory for the immutable form of this interface.
   */
  @API public interface Factory : HttpRequest.Factory {
    /**
     * Creates a new mutable HTTP request with the given method, path, and query parameters.
     *
     * @param method HTTP method for the request.
     * @param path Request path for the request.
     * @param query Query parameters for the request.
     * @return A new HTTP request with the given parameters.
     */
    override fun create(
      method: HttpMethod,
      path: HttpString,
      query: HttpMapping<HttpString, HttpString>?
    ): MutableHttpRequest

    /**
     * Copies an existing HTTP request into a new mutable request, with the option to override the method, path, and
     * query parameters.
     *
     * @param source Source request to copy.
     * @param method HTTP method for the request.
     * @param path Request path for the request.
     * @param query Query parameters for the request.
     * @return A new HTTP request with the given parameters.
     */
    override fun copy(
      source: HttpRequest,
      method: HttpMethod?,
      path: HttpString?,
      query: HttpMapping<HttpString, HttpString>?
    ): MutableHttpRequest
  }

  // Always mutable.
  override val mutable: Boolean get() = true

  /**
   *
   */
  override var method: HttpMethod

  /**
   *
   */
  override var path: HttpString

  /**
   *
   */
  override var url: URL

  /**
   *
   */
  override var query: MutableHttpMapping<HttpString, HttpString>
}
