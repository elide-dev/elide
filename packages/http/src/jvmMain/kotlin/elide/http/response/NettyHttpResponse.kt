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

import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpResponse
import elide.http.Body
import elide.http.Headers
import elide.http.MutableResponse
import elide.http.ProtocolVersion
import elide.http.Status
import elide.http.body.NettyBody
import elide.http.headers.NettyHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform HTTP response using Netty.
@JvmInline internal value class NettyHttpResponse (private val resp: HttpResponse): PlatformHttpResponse<HttpResponse> {
  override val response: HttpResponse get() = resp
  override val status: Status get() = NettyHttpStatus(resp.status())
  override val headers: Headers get() = NettyHttpHeaders(resp.headers())
  override val version: ProtocolVersion get() = resp.toProtocolVersion()

  override val trailers: Headers? get() = when (val resp = resp) {
    is FullHttpResponse -> NettyHttpHeaders(resp.trailingHeaders())
    else -> null
  }

  override val body: Body get() = when (val resp = resp) {
    is FullHttpResponse -> NettyBody(resp.content())
    else -> Body.Empty
  }

  override fun toMutable(): MutableResponse =
    NettyMutableHttpResponse(DefaultFullHttpResponse(
      resp.protocolVersion(),
      resp.status(),
      ByteBufAllocator.DEFAULT.buffer(),
      resp.headers(),
      when (resp) {
        is FullHttpResponse -> resp.trailingHeaders()
        else -> DefaultHttpHeaders()
      },
    ))
}
