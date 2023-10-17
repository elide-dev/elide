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
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.LastHttpContent
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.internal.GuestHandlerFunction
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
  override fun channelRead(channelContext: ChannelHandlerContext, message: Any) {
    // fast return
    if (message == LastHttpContent.EMPTY_LAST_CONTENT || message !is NettyHttpRequest) return

    // prepare the wrappers
    val request = NettyHttpRequest(message)
    val response = NettyHttpResponse(channelContext)

    // resolve the handler pipeline (or default to 'not found' if empty)
    router.pipeline(request).ifEmpty { DefaultPipeline }.forEach { handler ->
      handler(request, response)
    }
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ctx.close()
  }

  override fun channelReadComplete(ctx: ChannelHandlerContext) {
    ctx.flush()
  }

  private companion object {
    /** Default request handler used in absence of any matching pipeline stages. */
    private val DefaultHandler = GuestHandlerFunction { _, response ->
      // respond with 404 and end processing
      response.send(HttpResponseStatus.NOT_FOUND.code(), null)
      false
    }

    /** Cached pipeline used by default when no valid pipeline can be resolved from the router. */
    private val DefaultPipeline = sequenceOf(DefaultHandler)
  }
}
