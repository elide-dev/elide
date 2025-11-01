/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.http.server.netty

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.unix.Errors
import io.netty.handler.codec.DateFormatter
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil
import java.net.SocketException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logging
import elide.runtime.http.server.*
import elide.runtime.http.server.netty.NettyCallHandlerAdapter.RequestState.Dropping
import elide.runtime.http.server.netty.NettyCallHandlerAdapter.RequestState.Reading
import elide.runtime.http.server.netty.NettyCallHandlerAdapter.ResponseState.*

/**
 * Bidirectional channel handler that dispatches [HttpCall] instances to an [application].
 *
 * The [application] is used to create the [CallContext] attached to every new call before it is dispatched via
 * [HttpApplication.handle].
 *
 * Exceptions thrown during handling or I/O use will cause a `500` response to be sent if the response headers have not
 * been sent, or will close the connection if the response is already in progress.
 *
 * This handler is compatible with HTTP/1.x, requiring an [HttpServerCodec] and an [HttpServerExpectContinueHandler]
 * before it in the pipeline. Support for HTTP/2 is available when wrapped together with
 * [Http2StreamFrameToHttpObjectCodec][io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec] in an
 * [Http2MultiplexHandler][io.netty.handler.codec.http2.Http2MultiplexHandler] behind an
 * [Http2FrameCodec][io.netty.handler.codec.http2.Http2FrameCodec].
 */
internal class NettyCallHandlerAdapter(
  private val application: HttpApplication<CallContext>,
  private val defaultServerName: String,
) : ChannelDuplexHandler() {
  /**
   * Implements the [HttpCall] contract bound to the handler's [channelContext], setting the [requestState] and
   * [responseState] flags as the call's lifecycle methods are called.
   *
   * Closing the call will also close the [requestBody] and [responseBody] streams.
   */
  private inner class NettyHttpCall(
    override val context: CallContext,
    override val request: HttpRequest,
    override val response: HttpResponse,
    override val requestBody: NettyContentStream,
    override val responseBody: NettyContentStream,
    private val channelContext: ChannelHandlerContext,
  ) : HttpCall<CallContext> {
    private val requestWriter = AtomicReference<WritableContentStream.Writer?>(null)
    private val closed = AtomicBoolean(false)

    init {
      attachRequestSource()
    }

    private inline fun runInEventLoop(crossinline block: () -> Unit) {
      channelContext.executor().execute { block() }
    }

    private fun attachRequestSource() = requestBody.source(
      onAttached = { requestWriter.set(it) },
      onClose = { error ->
        requestWriter.set(null)
        close(error)
      },
      onPull = { channelContext.read() },
    )

    private fun consumeResponse() = responseBody.consume(
      onClose = { write(LastHttpContent.EMPTY_LAST_CONTENT, channelContext) },
      onRead = { bytes, reader ->
        write(DefaultHttpContent(bytes.retain()), channelContext)
        reader.pull()
      },
    )

    override fun sendHeaders() = runInEventLoop {
      if (responseState == Idle) {
        responseState = SendingHeaders
        write(response, channelContext)
      }
    }

    override fun send() = runInEventLoop {
      when (responseState) {
        Idle -> {
          // send headers first, then pull the body stream without waiting
          responseState = FlushingBody
          write(response, channelContext)
          consumeResponse()
        }

        SendingHeaders -> {
          // headers already flushed, pull the body stream
          responseState = FlushingBody
          consumeResponse()
        }

        // noop, we're already sending the body
        FlushingBody, SendingBody, Sent -> Unit
      }
    }

    fun offer(content: HttpContent): Boolean {
      return requestWriter.get()?.let { writer ->
        writer.write(content.content())
        if (content is LastHttpContent) writer.end()
      } != null
    }

    fun close(error: Throwable? = null) {
      if (!closed.compareAndSet(false, true)) return

      ReferenceCountUtil.release(request)
      requestBody.close()
      responseBody.close()
      context.callEnded(error)
    }

    override fun fail(cause: Throwable?) {
      runInEventLoop { if (inFlight === this) exceptionCaught(channelContext, cause) }
    }
  }

  private enum class RequestState {
    Reading,
    Dropping,
  }

  private enum class ResponseState {
    Idle,
    SendingHeaders,
    FlushingBody,
    SendingBody,
    Sent,
  }

  /** State of the incoming content; can be set to [Dropping] to discard all data until the next request. */
  private var requestState: RequestState = Reading

  /**
   * State of the outgoing content, indicates whether the [inFlight] call is idle (waiting for the response to start),
   * sending response headers, sending the response body, or closed ([inFlight] is `null`).
   */
  private var responseState: ResponseState = Idle

  /** Reference to the call currently being processed. */
  private var inFlight: NettyHttpCall? = null

  /** Whether the last [inFlight] call failed. */
  private var callFailed: Boolean = false

  override fun handlerAdded(ctx: ChannelHandlerContext) {
    // we need to set reads to manual so we can enforce backpressure
    ctx.channel().config().isAutoRead = false
    ctx.channel().read()
  }

  override fun handlerRemoved(ctx: ChannelHandlerContext?) {
    // cleanup open calls
    inFlight?.close()
    inFlight = null

    requestState = Dropping
    responseState = Idle
  }

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    when (msg) {
      is HttpRequest -> handleIncomingRequest(msg, ctx)
      is HttpContent -> if (requestState != Dropping) handleIncomingContent(msg, ctx)
      else ReferenceCountUtil.release(msg)

      else -> super.channelRead(ctx, msg)
    }
  }

  override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable?) {
    if (cause != null && shouldSuppress(cause)) {
      logging.trace("Exception ignored: $cause")
      return
    }

    // allow high-level APIs to see the error, for debugging and testing purposes
    runCatching { application.onError(cause) }
      .onFailure { logging.debug("Application failed to observe caught exception", it) }

    // only the first exception thrown during a call can fail it
    if (callFailed) return logging.error("Exception after failed call", cause)
    else if (inFlight != null) logging.error("Call failed with an exception", cause)
    else {
      // we can't recover from an error if there is no call to respond to
      logging.error("Exception caught outside of call dispatch", cause)
      context.close()
    }

    val call = inFlight ?: return
    callFailed = true

    when (responseState) {
      // if the error is raised before sending headers, we can send a 500 response
      Idle -> context.writeAndFlush(serverErrorResponseFor(call.request))
      // if the headers have been sent already, all we can do is close the connection
      else -> context.close()
    }

    closeInFlight(call, context, cause)
  }

  private fun write(message: HttpObject, context: ChannelHandlerContext) {
    val call = inFlight

    if (call == null) {
      // late writes can happen if a call fails; in those cases, dropping
      // the data is all we can do, since it can't be delivered anyway
      ReferenceCountUtil.release(message)
      return
    }

    when (message) {
      is HttpResponse -> {
        call.context.headersFlushed()
        context.writeAndFlush(message).addListener {
          // if the call is still active, notify the context
          if (inFlight === call) runCatching { call.context.headersSent() }
            .onFailure { exceptionCaught(context, it) }
        }
      }

      is HttpContent -> {
        if (responseState == FlushingBody) {
          // this is the first chunk of the content
          responseState = SendingBody
          call.context.contentFlushed()
        }

        context.writeAndFlush(message).addListener {
          if (inFlight !== call) return@addListener

          runCatching {
            if (message is LastHttpContent) {
              // EOS, finalize the call
              call.context.contentSent()
              closeInFlight(call, context)
            }
          }.onFailure {
            exceptionCaught(context, it)
          }
        }
      }

      // writing full responses in one message or other content is not allowed
      else -> releaseAndFail(message, "writing unsupported message $message")
    }
  }

  private fun handleIncomingRequest(request: HttpRequest, context: ChannelHandlerContext) {
    if (inFlight != null) releaseAndFail(request, "cannot accept new call while previous call is active")

    // reset I/O state
    requestState = Reading
    responseState = Idle
    callFailed = false

    // prepare call and store handle
    val call = newCall(request, context)
    inFlight = call

    // dispatch call, exceptions are handled via `exceptionCaught()`
    application.handle(call)
  }

  private fun handleIncomingContent(content: HttpContent, context: ChannelHandlerContext) {
    val call = inFlight ?: releaseAndFail(content, "cannot accept content without an active call")

    // if the content is rejected, discard the rest of the request
    if (!call.offer(content)) {
      requestState = Dropping
      content.release()
      context.read()
    }
  }

  private fun newCall(request: HttpRequest, channelContext: ChannelHandlerContext): NettyHttpCall {
    val requestBody = NettyContentStream(channelContext.executor())
    val responseBody = NettyContentStream(channelContext.executor())

    val response = defaultResponseFor(request, defaultServerName)
    val callContext = application.newContext(request, response, requestBody, responseBody)

    return NettyHttpCall(
      context = callContext,
      channelContext = channelContext,
      request = request,
      requestBody = requestBody,
      response = response,
      responseBody = responseBody,
    )
  }

  private fun closeInFlight(call: NettyHttpCall, context: ChannelHandlerContext, failure: Throwable? = null) {
    // if we're dealing with HTTP/1.0 close the connection
    if (shouldCloseAfter(call.request)) {
      context.close()
      return
    }

    requestState = Dropping
    responseState = Sent
    call.close()

    call.context.callEnded(failure)
    inFlight = null

    context.read() // keep reading
  }

  private fun releaseAndFail(content: Any, message: String): Nothing {
    ReferenceCountUtil.release(content)
    error("Internal error: $message")
  }

  internal companion object {
    private val logging = Logging.of(NettyCallHandlerAdapter::class)

    /** Prepare a base response for the given [request]. A call handler is expected to customize the returned value. */
    private fun defaultResponseFor(request: HttpRequest, serverName: String): HttpResponse = DefaultHttpResponse(
      /* version = */ request.protocolVersion(),
      /* status = */ HttpResponseStatus.OK,
    ).apply {
      setHeader(HttpHeaderNames.DATE, DateFormatter.format(Date()))
      setHeader(HttpHeaderNames.SERVER, serverName)
      HttpUtil.setContentLength(this, 0)
    }

    /** Prepare a default 500-ISE response for the given [request]. */
    private fun serverErrorResponseFor(request: HttpRequest): FullHttpResponse = DefaultFullHttpResponse(
      /* version = */ request.protocolVersion(),
      /* status = */ HttpResponseStatus.INTERNAL_SERVER_ERROR,
      /* content = */ Unpooled.EMPTY_BUFFER,
    ).apply {
      HttpUtil.setContentLength(this, 0)
      setHeader(HttpHeaderNames.CONNECTION, "close")
    }

    /**
     * Returns `true` if the given [throwable] should be suppressed and logged at the "trace" level instead of as an
     * error; this is used to ignore benign exceptions that have no effect on the handler's operation.
     */
    private fun shouldSuppress(throwable: Throwable): Boolean {
      return throwable is SocketException ||
              throwable is Errors.NativeIoException
    }

    /** Whether the connection should be closed after the call ends. */
    private fun shouldCloseAfter(request: HttpRequest): Boolean {
      if (request.getHeader(HttpHeaderNames.CONNECTION) == "close") return true

      if (
        request.protocolVersion() == HttpVersion.HTTP_1_0 &&
        request.getHeader(HttpHeaderNames.CONNECTION) != "keep-alive"
      ) return true

      return false
    }
  }
}
