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

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpVersion
import io.micronaut.http.MutableHttpResponse
import elide.http.Body
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Response
import elide.http.Status
import elide.http.body.MicronautBody
import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.headers.MicronautMutableHttpHeaders
import elide.http.micronaut.MicronautMutableHttpMessage
import elide.http.toProtocolVersion

// Implements a platform-specific mutable HTTP response type for Micronaut.
@JvmInline internal value class MicronautMutableHttpResponse<T>(private val data: MutableHttpResponse<T>)
  : MicronautMutableHttpMessage, PlatformMutableHttpResponse<HttpResponse<T>> {
  override val response: HttpResponse<T> get() = data
  override val httpVersion: HttpVersion get() = HttpVersion.HTTP_1_1  // @TODO http2 on response
  override val version: ProtocolVersion get() = httpVersion.toProtocolVersion()
  override val headers: MutableHeaders get() = MicronautMutableHttpHeaders(data.headers)
  override val trailers: MutableHeaders? get() = null // @TODO implement trailers
  override fun build(): Response = MicronautHttpResponse.of(data)

  override var status: Status
    get() = MicronautHttpStatus(data.status)
    set(value) {
      when (value) {
        is MicronautHttpStatus -> data.status(value.status)
        else -> data.status(value.code.symbol.toInt(), value.message)
      }
    }

  override var body: Body
    get() = when (data.body.isPresent) {
      false -> Body.Empty
      else -> MicronautBody.of(data)
    }
    set(value) {
      when (value) {
        is Body.Empty -> data.body(null)
        is MicronautBody<*> -> data.body(value.unwrap())
        is PrimitiveBody<*> -> data.body(value.unwrap())
        is NettyBody -> data.body(value.unwrap())
        else -> error("Unsupported body: $value")
      }
    }
}
