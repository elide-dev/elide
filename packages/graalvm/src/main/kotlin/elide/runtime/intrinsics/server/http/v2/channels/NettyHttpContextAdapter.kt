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
package elide.runtime.intrinsics.server.http.v2.channels

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil
import java.net.SocketException
import elide.runtime.Logging
import elide.runtime.intrinsics.server.http.v2.HttpContext
import elide.runtime.intrinsics.server.http.v2.HttpContextFactory
import elide.runtime.intrinsics.server.http.v2.HttpContextHandler

/**
 * A channel handler that creates [HttpContext] instances for incoming HTTP requests and manages async IO using
 * [NettyContextSource] and [NettyContextSink]. The context is passed to an HTTP routing pipeline for final handling
 * before the response is sent.
 *
 * After a request is initially decoded, all following [HttpContent] messages will be delivered to the
 * [HttpContext.requestBody] source.
 *
 * Once the HTTP context is handled (indicated using channel promises), the response will be sent to the client, and
 * then the [HttpContext.responseBody] sink will be drained into the channel.
 */
internal class NettyHttpContextAdapter(
  private val contextFactory: HttpContextFactory<HttpContext>,
  private val contextHandler: HttpContextHandler,
) : ChannelDuplexHandler() {
  /** Reference to the context for the HTTP request currently being handled. */
  private var activeContext: HttpContext? = null

  /** The request body source for the [activeContext]. */
  private inline val activeSource: NettyContextSource?
    get() = activeContext?.requestBody as NettyContextSource?

  /** The response body for the [activeContext].  */
  private inline val activeSink: NettyContextSink?
    get() = activeContext?.responseBody as NettyContextSink?

  /** Track whether inbound content should be dropped (e.g., after expectation failure). */
  private var ignoringContent: Boolean = false

  override fun channelActive(ctx: ChannelHandlerContext) {
    // we need to set reads to manual so we can enforce backpressure
    ctx.channel().config().isAutoRead = false
    ctx.channel().read()
  }

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (ignoringContent) {
      if (msg is HttpContent) {
        ReferenceCountUtil.release(msg)
        if (msg is LastHttpContent) ignoringContent = false
        return
      }
      ignoringContent = false
    }

    when (msg) {
      // if the message is a request, create a new context for it and set it as current
      is HttpRequest -> handleIncoming(ctx, msg)
      // if it's a content chunk, pass it to the current request's source
      is HttpContent -> activeSource?.handleRead(msg) ?: ReferenceCountUtil.release(msg)
      // otherwise let the next handler use it
      else -> super.channelRead(ctx, msg)
    }
  }

  override fun channelReadComplete(ctx: ChannelHandlerContext) {
    // if there is no active source, we can skip ahead until we get a new request;
    // if there is one, ask if it wants data to respect backpressure
    if (activeSource?.shouldRead() != false) ctx.channel().read()
  }

  override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    val shouldPullAfter = when (msg) {
      is HttpResponse -> true
      is LastHttpContent -> false
      is HttpContent -> true
      else -> false
    }

    if (!shouldPullAfter) closeCurrent(ctx)
    else promise.addListener { if (activeSink?.maybePull() != true) closeCurrent(ctx) }

    super.write(ctx, msg, promise)
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
    if (cause !is SocketException) logging.error("Unhandled exception in HTTP context", cause)
    else logging.error("Unhandled socket exception", cause)

    closeCurrent(ctx)
  }

  /** Handle a new incoming [request], closing the current one if active and initializing a new context. */
  private fun handleIncoming(channelContext: ChannelHandlerContext, request: HttpRequest) {
    closeCurrent(channelContext)

    if (!handleExpectations(channelContext, request)) {
      channelContext.channel().read()
      return
    }

    val requestSource = NettyContextSource(channelContext)
    val responseSink = NettyContextSink(channelContext)

    val httpContext = contextFactory.newContext(
      incomingRequest = request,
      channelContext = channelContext,
      requestSource = requestSource,
      responseSink = responseSink,
    ).also { activeContext = it }

    val completion = runCatching {
      contextHandler.handle(httpContext, ChannelScope(channelContext))
    }.getOrElse {
      logging.warn("Request handler crashed", it)
      val response = DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR)
      channelContext.channel().writeAndFlush(response)

      return
    }

    completion.addListener {
      val response = if (it.isSuccess) httpContext.response else {
        logging.warn("Request handler error", it.cause())
        DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR)
      }

      // send the response header, the body will be sent automatically after
      channelContext.channel().writeAndFlush(response)
    }
  }

  /**
   * Close the [activeContext], its source and sink, and close the connection to the client if required by the
   * protocol or headers.
   */
  private fun closeCurrent(channelContext: ChannelHandlerContext) {
    val currentRequest = activeContext?.request ?: return
    if (!HttpUtil.isKeepAlive(currentRequest))
      channelContext.close()

    activeContext?.close()
    activeSink?.close()
    activeSource?.close()
    activeContext = null
    ignoringContent = false
  }

  private fun handleExpectations(
    channelContext: ChannelHandlerContext,
    request: HttpRequest,
  ): Boolean {
    val response = when {
      isUnsupportedExpectation(request) -> {
        channelContext.pipeline().fireUserEventTriggered(HttpExpectationFailedEvent.INSTANCE)
        expectationFailedResponse()
      }

      HttpUtil.is100ContinueExpected(request) -> continueResponse()
      else -> null
    } ?: return true

    request.headers().remove(HttpHeaderNames.EXPECT)

    val shouldContinue = response.status().codeClass() != HttpStatusClass.CLIENT_ERROR
    if (!shouldContinue) ignoringContent = true

    channelContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE)

    return shouldContinue
  }

  private fun isUnsupportedExpectation(message: HttpMessage): Boolean {
    if (!isExpectHeaderValid(message)) return false

    val expectValue = message.headers().get(HttpHeaderNames.EXPECT)
    return expectValue != null && !HttpHeaderValues.CONTINUE.toString().equals(expectValue, ignoreCase = true)
  }

  private fun isExpectHeaderValid(message: HttpMessage): Boolean {
    return message is HttpRequest && message.protocolVersion() >= HttpVersion.HTTP_1_1
  }

  companion object {
    private val logging by lazy { Logging.of(NettyHttpContextAdapter::class) }

    private fun continueResponse(): FullHttpResponse = DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.CONTINUE,
      Unpooled.EMPTY_BUFFER,
    ).apply { HttpUtil.setContentLength(this, 0) }

    private fun expectationFailedResponse(): FullHttpResponse = DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.EXPECTATION_FAILED,
      Unpooled.EMPTY_BUFFER,
    ).apply { HttpUtil.setContentLength(this, 0) }
  }
}
