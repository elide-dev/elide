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

package elide.runtime.intrinsics.server.http.netty

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse

/** [HttpRequest] implementation wrapping a Netty handler context. */
@DelicateElideApi internal class NettyHttpResponse(private val context: ChannelHandlerContext) : HttpResponse {
  /** Headers for this response, dispatched once the response is sent to the client. */
  private val headers = DefaultHttpHeaders(false)

  @Export override fun send(status: Int, body: PolyglotValue?) {
    send(HttpResponseStatus.valueOf(status), body)
  }

  /**
   * Sends a response with the provided [status] and [body], allocating a buffer for the content as necessary.
   *
   * @param status The HTTP status code of the response.
   * @param body A guest value to be unwrapped and used as response content.
   */
  private fun send(status: HttpResponseStatus, body: PolyglotValue?) {
    // TODO: support JSON objects and other types of content
    // treat any type of value as a string (force conversion)
    val content = body?.let { Unpooled.wrappedBuffer(it.toString().toByteArray()) } ?: Unpooled.EMPTY_BUFFER

    // prepare headers
    headers
      .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
      .set(HttpHeaderNames.SERVER, "Elide")

    // prepare the response object
    val response = if (content != null) {
      headers.set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex())

      DefaultFullHttpResponse(
        /* version = */ HttpVersion.HTTP_1_1,
        /* status = */ status,
        /* content = */ content,
        /* headers = */ headers,
        /* trailingHeaders = */ EmptyHttpHeaders.INSTANCE,
      )
    } else {
      DefaultHttpResponse(
        /* version = */ HttpVersion.HTTP_1_1,
        /* status = */ status,
        /* headers = */ headers,
      )
    }

    // send the response
    context.write(response)
  }
}
