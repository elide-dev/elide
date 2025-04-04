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

package elide.http.response

import java.net.http.HttpRequest
import java.net.http.HttpResponse
import elide.http.Body
import elide.http.Headers
import elide.http.MutableResponse
import elide.http.ProtocolVersion
import elide.http.Status
import elide.http.StatusCode
import elide.http.body.PrimitiveBody
import elide.http.body.PublisherBody
import elide.http.headers.JavaNetHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP response type for `java.net.http`.
@JvmInline internal value class JavaNetHttpResponse<T>(private val backing: HttpResponse<T>)
  : PlatformHttpResponse<HttpResponse<T>> {
  override val response: HttpResponse<T> get() = backing
  override val version: ProtocolVersion get() = backing.toProtocolVersion()
  override val status: Status get() = Status.of(StatusCode.resolve(backing.statusCode().toUShort()))
  override val headers: Headers get() = JavaNetHttpHeaders(backing.headers())
  override val trailers: Headers? get() = null // @TODO(sgammon): trailers support
  override fun toMutable(): MutableResponse = JavaNetMutableHttpResponse(backing)
  override val body: Body get() = when (val body = backing.body()) {
    null -> Body.Empty
    is Body.Empty -> Body.Empty
    is PublisherBody -> body
    is HttpRequest.BodyPublisher -> when (body.contentLength()) {
      0L -> Body.Empty
      else -> PublisherBody(body)
    }
    is String -> PrimitiveBody.string(body)
    is ByteArray -> PrimitiveBody.bytes(body)
    else -> error("Unsupported body type: $body")
  }
}
