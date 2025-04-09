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
import elide.http.Headers
import elide.http.Http
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableRequest
import elide.http.ProtocolVersion
import elide.http.body.NettyBody
import elide.http.headers.NettyHttpHeaders

// Implements a `PlatformHttpRequest` backed by a raw Netty HTTP request.
@JvmInline internal value class NettyHttpRequest(
  private val pair: Pair<HttpRequest, Http.HttpRequestOptions?>,
): PlatformHttpRequest<HttpRequest> {
  constructor(req: HttpRequest): this(req to null)

  override val request: HttpRequest get() = pair.first
  override val method: Method get() = NettyHttpMethod(pair.first.method())
  override val url: HttpUrl get() = JavaNetHttpUri.from(URI.create(pair.first.uri()), pair.second)
  override val headers: Headers get() = NettyHttpHeaders(pair.first.headers())
  override fun toMutable(): MutableRequest = NettyMutableHttpRequest(pair.first)

  override val body: Body get() = when (val req = pair.first) {
    is FullHttpRequest -> if (req.content().readableBytes() > 0) {
      NettyBody(req.content())
    } else {
      Body.Empty
    }

    else -> Body.Empty
  }

  override val version: ProtocolVersion get() = when (pair.first.protocolVersion().majorVersion()) {
    2 -> ProtocolVersion.HTTP_2
    else -> when (pair.first.protocolVersion().minorVersion()) {
      0 -> ProtocolVersion.HTTP_1_0
      1 -> ProtocolVersion.HTTP_1_1
      else -> ProtocolVersion.HTTP_1_0
    }
  }

  companion object {
    @JvmStatic fun from(req: HttpRequest, options: Http.HttpRequestOptions?): NettyHttpRequest =
      NettyHttpRequest(req to options)
  }
}
