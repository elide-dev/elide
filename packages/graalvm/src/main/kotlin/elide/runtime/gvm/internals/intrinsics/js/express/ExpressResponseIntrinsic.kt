/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics.js.express

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Value
import reactor.netty.http.server.HttpServerResponse
import elide.runtime.intrinsics.js.express.ExpressResponse

/** An [ExpressResponse] implemented as a wrapper around a Reactor Netty [HttpServerResponse]. */
internal class ExpressResponseIntrinsic(private val context: ChannelHandlerContext) : ExpressResponse {
  private val headers = DefaultHttpHeaders(false)

  /** Send part of the request content, automatically flushing the headers if necessary. */
  private fun sendBody(content: ByteBuf) {
    headers
      .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
      .set(HttpHeaderNames.SERVER, "Elide")
      .set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex())

    val response = DefaultFullHttpResponse(
      /* version = */ HttpVersion.HTTP_1_1,
      /* status = */ HttpResponseStatus.OK,
      /* content = */ content,
      /* headers = */ headers,
      /* trailingHeaders = */ EmptyHttpHeaders.INSTANCE,
    )

    context.write(response)
  }

  override fun send(body: Value) {
    // TODO: support JSON objects and other types of content
    // treat any type of value as a string (force conversion)
    sendBody(Unpooled.wrappedBuffer(body.toString().toByteArray()))
  }
}
