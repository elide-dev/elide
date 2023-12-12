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

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

/**
 * A custom shareable (thread-safe) handler used to bridge the Netty server with guest code.
 *
 * Given the thread-local approach used by the the server intrinsics, a single [NettyRequestHandler] can be safely
 * used from different threads (hence the [@Sharable][Sharable] marker).
 */
@DelicateElideApi @Sharable internal class NettyRequestHandler(
  private val router: PipelineRouter
) : ChannelInboundHandlerAdapter() {
  /** Handler-scoped logger. */
  private val logging by lazy { Logging.of(NettyRequestHandler::class) }

  override fun channelRead(channelContext: ChannelHandlerContext, message: Any) {
    // fast return
    if (message == LastHttpContent.EMPTY_LAST_CONTENT || message !is NettyHttpRequest) return

    runCatching {
      logging.debug { "Handling HTTP request: $message" }

      // prepare the wrappers
      val request = NettyHttpRequest(message)
      val response = NettyHttpResponse(channelContext)
      val context = HttpContext()

      // resolve the handler pipeline (or default to 'not found' if empty)
      logging.debug("Resolving HTTP pipeline")
      router.pipeline(request, context).forEach { handler ->
        handler(request, response, context)
      }

      logging.debug("Request processing complete, flushing response")
      response.also { it.send(404, null) }
    }.onFailure { cause ->
      logging.error { "Error handling request: ${cause.stackTraceToString()}" }
      channelContext.writeAndFlush(
        DefaultHttpResponse(
          /* version = */ HttpVersion.HTTP_1_1,
          /* status = */ HttpResponseStatus.INTERNAL_SERVER_ERROR,
        ),
      )
      channelContext.close()
    }
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ctx.close()
  }

  override fun channelReadComplete(ctx: ChannelHandlerContext) {
    ctx.flush()
  }
}
