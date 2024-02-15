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
import elide.http.api.HttpMessageType.RESPONSE

/**
 * # HTTP: Response
 *
 * Describes the basic API provided for HTTP responses within Elide; this extends [HttpMessage] with information which
 * is specific to responses.
 *
 * ## Features
 *
 * TBD.
 *
 * ## Response Properties
 *
 * ### Status ([status])
 *
 * Describes the HTTP status for the response expressed by this record; response statuses carry a status code, and an
 * optional text "reason" or description.
 *
 * ### Trailers ([trailers])
 *
 * Describes the HTTP trailers for the response, if any. Trailers are a set of headers which are sent after the response
 * body, and are used to carry metadata about the response. Trailers are not supported by all clients or servers, and
 * only at HTTP/2 or later.
 *
 * @see HttpMessage for fields and methods shared with HTTP messages.
 * @see HttpRequest for the request counterpart to this interface.
 * @see MutableHttpResponse for the mutable form of this interface
 */
@API public interface HttpResponse : HttpMessage {
  /**
   * # HTTP Response: Factory
   */
  @API public interface Factory {

  }

  override val type: HttpMessageType get() = RESPONSE

  /**
   * Describes the HTTP status for the response expressed by this record; response statuses carry a status code, and an
   * optional text "reason" or description.
   */
  public val status: HttpStatus

  /**
   * Describes the HTTP trailers for the response, if any. Trailers are a set of headers which are sent after the
   * response body, and are used to carry metadata about the response. Trailers are not supported by all clients or
   * servers, and only at HTTP/2 or later.
   */
  public val trailers: HttpHeaders
}
