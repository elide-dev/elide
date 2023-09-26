package elide.runtime.intrinsics.server.http.netty

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.LastHttpContent
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpContext
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
    val context = HttpContext()

    // resolve the handler pipeline (or default to 'not found' if empty)
    router.pipeline(request, context).ifEmpty { DefaultPipeline }.forEach { handler ->
      handler(request, response, context)
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
    private val DefaultHandler = GuestHandlerFunction { _, response, _ ->
      // respond with 404 and end processing
      response.send(HttpResponseStatus.NOT_FOUND.code(), null)
      false
    }

    /** Cached pipeline used by default when no valid pipeline can be resolved from the router. */
    private val DefaultPipeline = sequenceOf(DefaultHandler)
  }
}