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

import io.micronaut.http.HttpMethod
import elide.core.api.Symbolic

// HTTP method constants.
private const val HTTP_METHOD_GET: String = "GET"
private const val HTTP_METHOD_HEAD: String = "HEAD"
private const val HTTP_METHOD_POST: String = "POST"
private const val HTTP_METHOD_PUT: String = "PUT"
private const val HTTP_METHOD_DELETE: String = "DELETE"
private const val HTTP_METHOD_OPTIONS: String = "OPTIONS"
private const val HTTP_METHOD_PATCH: String = "PATCH"
private const val HTTP_METHOD_TRACE: String = "TRACE"
private const val HTTP_METHOD_CONNECT: String = "CONNECT"

/**
 * ## Standard HTTP Method
 *
 * Provides an enumeration of standard HTTP method verbs, and their use constraints/disposition.
 */
public actual enum class StandardMethod (actual override val symbol: String) : Method.PlatformMethod, Symbolic<String> {
  GET(HTTP_METHOD_GET),
  HEAD(HTTP_METHOD_HEAD),
  POST(HTTP_METHOD_POST),
  PUT(HTTP_METHOD_PUT),
  DELETE(HTTP_METHOD_DELETE),
  OPTIONS(HTTP_METHOD_OPTIONS),
  PATCH(HTTP_METHOD_PATCH),
  TRACE(HTTP_METHOD_TRACE),
  CONNECT(HTTP_METHOD_CONNECT);

  private fun asMicronaut(): HttpMethod = when (this) {
    GET -> HttpMethod.GET
    HEAD -> HttpMethod.HEAD
    POST -> HttpMethod.POST
    PUT -> HttpMethod.PUT
    DELETE -> HttpMethod.DELETE
    OPTIONS -> HttpMethod.OPTIONS
    PATCH -> HttpMethod.PATCH
    TRACE -> HttpMethod.TRACE
    CONNECT -> HttpMethod.CONNECT
  }

  actual override val permitsRequestBody: Boolean get() = asMicronaut().permitsRequestBody()
  actual override val permitsResponseBody: Boolean get() = asMicronaut().permitsResponseBody()
  actual override val requiresRequestBody: Boolean get() = asMicronaut().requiresRequestBody()

  public actual companion object : Symbolic.SealedResolver<String, StandardMethod> {
    public val all: Sequence<StandardMethod> = sequence {
      yieldAll(StandardMethod.entries)
    }

    actual override fun resolve(symbol: String): StandardMethod = when (symbol) {
      HTTP_METHOD_GET -> GET
      HTTP_METHOD_HEAD -> HEAD
      HTTP_METHOD_POST -> POST
      HTTP_METHOD_PUT -> PUT
      HTTP_METHOD_DELETE -> DELETE
      HTTP_METHOD_OPTIONS -> OPTIONS
      HTTP_METHOD_PATCH -> PATCH
      HTTP_METHOD_TRACE -> TRACE
      HTTP_METHOD_CONNECT -> CONNECT
      else -> throw unresolved("Unknown HTTP method: $symbol")
    }
  }
}
