package elide.runtime.intriniscs.server.http.netty

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.LastHttpContent
import elide.runtime.core.DelicateElideApi
import elide.runtime.intriniscs.server.http.internal.GuestHandlerFunction
import elide.runtime.intriniscs.server.http.internal.HttpRequest
import elide.runtime.intriniscs.server.http.internal.HttpRequestContext
import elide.runtime.intriniscs.server.http.internal.HttpResponse
import elide.runtime.intriniscs.server.http.internal.HttpRouter
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

@DelicateElideApi @Sharable internal class NettyRequestHandler(
  private val router: HttpRouter
) : ChannelInboundHandlerAdapter() {
  @Suppress("unused_parameter")
  private fun handleNotFound(request: HttpRequest, response: HttpResponse, context: HttpRequestContext) {
    response.send(HttpResponseStatus.NOT_FOUND)
  }

  override fun channelRead(channelContext: ChannelHandlerContext, message: Any) {
    // fast return
    if (message == LastHttpContent.EMPTY_LAST_CONTENT || message !is NettyHttpRequest) return

    // prepare the wrappers
    val request = HttpRequest(message)
    val response = HttpResponse(channelContext)
    val context = HttpRequestContext()

    // resolve the handler (or default to 'not found')
    // TODO: to support a full handler stack, an iterable should be returned here
    val handler: GuestHandlerFunction = router.route(request, context) ?: ::handleNotFound

    // dispatch the request
    handler(request, response, context)
  }

  @Deprecated("Deprecated in Java")
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    ctx.close()
  }

  override fun channelReadComplete(ctx: ChannelHandlerContext) {
    ctx.flush()
  }
}