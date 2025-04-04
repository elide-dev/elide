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

import io.netty.buffer.ByteBufInputStream
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import elide.http.Body
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Response
import elide.http.Status
import elide.http.StatusCode
import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.body.PublisherBody
import elide.http.headers.JavaNetMutableHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP response type for `java.net.http` with mutability.
internal class JavaNetMutableHttpResponse<T>(
  private var data: HttpResponse<T>,
): PlatformMutableHttpResponse<HttpResponse<T>> {
  override val response: HttpResponse<T> get() = data
  override val version: ProtocolVersion get() = data.toProtocolVersion()
  override val headers: MutableHeaders = JavaNetMutableHttpHeaders(data.headers())
  override val trailers: MutableHeaders? = null // @TODO(sgammon): trailers support
  override fun build(): Response = JavaNetHttpResponse(data)

  override var status: Status
    get() = Status.of(StatusCode.resolve(data.statusCode().toUShort()))
    set(value) {
      data = JdkHttp.builder(value.code)
        .apply {
          request = data.request()
          sslSession = data.sslSession()
          headers.putAll(data.headers().map())
        }
        .build()
    }

  override var body: Body
    get() = when (val body = data.body()) {
      null, is Body.Empty -> Body.Empty
      is PublisherBody -> body
      is HttpRequest.BodyPublisher -> when (body.contentLength()) {
        0L -> Body.Empty
        else -> PublisherBody(body)
      }
      else -> error("Unsupported body type: $body")
    }
    set(value) {
      data = JdkHttp.builder(StatusCode.resolve(data.statusCode().toUShort()))
        .apply {
          request = data.request()
          sslSession = data.sslSession()
          headers.putAll(data.headers().map())
          body = when (value) {
            is Body.Empty -> null
            is PublisherBody -> value.unwrap()
            is PrimitiveBody.StringBody -> HttpRequest.BodyPublishers.ofString(value.unwrap())
            is PrimitiveBody.Bytes -> HttpRequest.BodyPublishers.ofByteArray(value.unwrap())
            is NettyBody -> HttpRequest.BodyPublishers.ofInputStream { ByteBufInputStream(value.unwrap()) }
            else -> error("Unsupported body type: $value")
          }
        }
        .build<T>()
    }
}
