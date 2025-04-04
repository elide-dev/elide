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

package elide.http.request

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import java.net.URI
import elide.http.Body
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Request
import elide.http.body.NettyBody
import elide.http.headers.NettyMutableHttpHeaders

// Implements a `PlatformHttpRequest` backed by a raw Netty HTTP request, with mutability.
@JvmInline internal value class NettyMutableHttpRequest(private val req: HttpRequest)
  : PlatformMutableHttpRequest<HttpRequest> {
  override val request: HttpRequest get() = req
  override val method: Method get() = NettyHttpMethod(req.method())
  override val headers: MutableHeaders get() = NettyMutableHttpHeaders(req.headers())
  override fun build(): Request = NettyHttpRequest(req)

  override val version: ProtocolVersion get() = when (req.protocolVersion().majorVersion()) {
    2 -> ProtocolVersion.HTTP_2
    else -> when (req.protocolVersion().minorVersion()) {
      0 -> ProtocolVersion.HTTP_1_0
      1 -> ProtocolVersion.HTTP_1_1
      else -> ProtocolVersion.HTTP_1_0
    }
  }

  override var url: HttpUrl
    get() = JavaNetHttpUri(URI.create(req.uri()))
    set(value) {
      when (value) {
        is JavaNetHttpUri -> req.setUri(value.value.toString())
        else -> error("Unsupported URL type: ${value::class.java.name}")
      }
    }

  override var body: Body
    get() = when (req) {
      is FullHttpRequest -> NettyBody(req.content())
      else -> Body.Empty
    }
    set(value) {
      when (value) {
        is NettyBody -> when (req) {
          is FullHttpRequest -> req.content().writeBytes(value.unwrap())
          else -> error("Cannot assign body: mutable Netty HTTP request is not full")
        }
        is Body.Empty -> when (req) {
          is FullHttpRequest -> req.content().clear()
          else -> {}  // nothing to do
        }
        else -> error("Unsupported body type: ${value::class.java.name}")
      }
    }
}
