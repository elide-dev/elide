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
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import java.nio.charset.StandardCharsets
import elide.http.Body
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Response
import elide.http.Status
import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.headers.NettyMutableHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform HTTP response using Netty with mutability.
@JvmInline public value class NettyMutableHttpResponse internal constructor (private val resp: FullHttpResponse):
  PlatformMutableHttpResponse<HttpResponse> {
  override val response: HttpResponse get() = resp
  override fun build(): Response = NettyHttpResponse(resp)
  override val headers: MutableHeaders get() = NettyMutableHttpHeaders(resp.headers())
  override val version: ProtocolVersion get() = resp.toProtocolVersion()
  override val trailers: MutableHeaders get() = NettyMutableHttpHeaders(resp.trailingHeaders())

  override var status: Status
    get() = NettyHttpStatus(resp.status())
    set(value) {
      when (value) {
        is NettyHttpStatus -> resp.status = value.status
        else -> resp.status = HttpResponseStatus.valueOf(value.code.symbol.toInt(), value.message ?: "")
      }
    }

  override var body: Body
    get() = when {
      resp.content().readableBytes() > 0 -> NettyBody(resp.content())
      else -> Body.Empty
    }

    set(value) {
      when (value) {
        is NettyBody -> resp.content().writeBytes(value.unwrap())
        is PrimitiveBody.StringBody -> resp.content().writeCharSequence(value.unwrap(), StandardCharsets.UTF_8)
        is PrimitiveBody.Bytes -> resp.content().writeBytes(value.unwrap())
        is Body.Empty -> resp.content().clear()
        else -> error("Unsupported body type: ${value::class.simpleName}")
      }
    }

  public companion object {
    @JvmStatic public fun empty(): NettyMutableHttpResponse = NettyMutableHttpResponse(
      DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        ByteBufAllocator.DEFAULT.buffer(),
        DefaultHttpHeaders(),
        DefaultHttpHeaders(),
      ),
    )
  }
}
