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
package elide.runtime.intrinsics.server.http.netty

import com.oracle.truffle.api.exception.AbstractTruffleException
import com.oracle.truffle.api.interop.TruffleObject
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import org.graalvm.polyglot.Value
import kotlinx.atomicfu.atomic
import elide.http.Body
import elide.http.Headers
import elide.http.Http
import elide.http.ProtocolVersion
import elide.http.Response
import elide.http.Status
import elide.http.body.NettyBody
import elide.http.response.NettyMutableHttpResponse
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchResponseIntrinsic
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.internal.GuestAsyncHandler
import elide.runtime.intrinsics.server.http.internal.GuestSimpleHandler
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

// Express an HTTP version as a Netty protocol version.
private fun ProtocolVersion.toNettyProtocolVersion() = when (this) {
  ProtocolVersion.HTTP_1_0 -> HttpVersion.HTTP_1_0
  else -> HttpVersion.HTTP_1_1
}

// Convert a universal HTTP status from Elide to a Netty status.
private fun Status.toNettyStatus(): HttpResponseStatus = HttpResponseStatus.valueOf(code.symbol.toInt(), message)

// Convert a universal HTTP headers from Elide to a Netty headers.
private fun Headers.toNettyHeaders(): HttpHeaders {
  return DefaultHttpHeaders().apply {
    asOrdered().forEach { header ->
      header.value.values.forEach { value ->
        add(header.name.name, value)
      }
    }
  }
}

// Convert a universal HTTP body from Elide to a Netty body.
private fun Body.toNettyBody(): ByteBuf = when (this) {
  is NettyBody -> unwrap()
  else -> error("Unrecognized body type: $this")
}

// Convert fetch headers to Netty headers.
private fun FetchHeaders.asNetty(): HttpHeaders {
  val hdrs = DefaultHttpHeaders()
  keys.forEach { key ->
    getAll(key).forEach { value ->
      hdrs.add(key, value)
    }
  }
  return hdrs
}

// Convert a guest-constructed fetch-style response to a Netty response.
private fun FetchResponseIntrinsic.asNetty(): HttpResponse {
  val headers = headers.asNetty()
  val version = HttpVersion.HTTP_1_1
  val status = if (statusText.isNotEmpty()) {
    HttpResponseStatus.valueOf(status, statusText)
  } else {
    HttpResponseStatus.valueOf(status)
  }

  return if (bodyPresent) ByteBufAllocator.DEFAULT.buffer().let { content ->
    // @TODO: write body
    DefaultFullHttpResponse(
      /* version = */ version,
      /* status = */ status,
      /* content = */ content,
      /* headers = */ headers,
      /* trailingHeaders = */ DefaultHttpHeaders(),
    )
  } else DefaultHttpResponse(
    /* version = */ version,
    /* status = */ status,
    /* headers = */ headers,
  )
}

// Convert a universal HTTP response from Elide to a Netty response.
private fun Response.asNetty(): HttpResponse = when (body) {
  is Body.Empty -> DefaultHttpResponse(
    /* version = */ version.toNettyProtocolVersion(),
    /* status = */ status.toNettyStatus(),
    /* headers = */ headers.toNettyHeaders(),
  )

  else -> DefaultFullHttpResponse(
    /* version = */ version.toNettyProtocolVersion(),
    /* status = */ status.toNettyStatus(),
    /* content = */ body.toNettyBody(),
    /* headers = */ headers.toNettyHeaders(),
    /* trailingHeaders = */ trailers?.toNettyHeaders() ?: DefaultHttpHeaders(),
  )
}

/**
 * A custom shareable (thread-safe) handler used to bridge the Netty server with guest code.
 *
 * Given the thread-local approach used by the server intrinsics, a single [NettyRequestHandler] can be safely
 * used from different threads (hence the [@Sharable][Sharable] marker).
 */
@DelicateElideApi @Sharable internal class NettyRequestHandler(
  private val router: PipelineRouter,
  private val exec: GuestExecutorProvider,
  private val scheme: String,
  private val host: String,
  private val port: UShort,
) : ChannelInboundHandlerAdapter() {
  /** Handler-scoped logger. */
  private val logging by lazy { Logging.of(NettyRequestHandler::class) }

  // Handles a response from a guest handler.
  @JvmInline private value class PolyglotResponseHandler(private val value: PolyglotValue) {
    fun ChannelHandlerContext.sendError(
      logging: Logger,
      version: HttpVersion,
      code: HttpResponseStatus,
      message: String,
    ) {
      logging.error {
        "Failed to execute server handler: $message. Returning ${code.code()}."
      }
      writeAndFlush(
        DefaultHttpResponse(
          /* version = */ version,
          /* status = */ code,
        ),
      )
      close()
    }

    fun send(
      version: HttpVersion,
      logging: Logger,
      ctx: ChannelHandlerContext,
    ) {
      val promise = try {
        value.`as`<JSPromiseObject>(JSPromiseObject::class.java)
      } catch (err: ClassCastException) {
        ctx.sendError(
          logging,
          version,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to process response: ${err.message}",
        )
        return
      }
      when (val result = when (val result = promise.promiseResult) {
        is TruffleObject -> Value.asValue(result)
        else -> result
      }) {
        null -> ctx.sendError(
          logging,
          version,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to process response: Handler returned null promise result",
        )

        is Response -> logging.debug { "Guest handler returned Elide response: $result" }.also {
          ctx.writeAndFlush(result.asNetty())
          ctx.close()
        }

        is HttpResponse -> logging.debug { "Guest handler returned Netty response: $result" }.also {
          ctx.writeAndFlush(result)
          ctx.close()
        }

        is Value -> when {
          result.isHostObject -> result.asHostObject<FetchResponseIntrinsic>().let { response ->
            logging.debug { "Guest handler returned Elide response: $response" }.also {
              ctx.writeAndFlush(response.asNetty())
              ctx.close()
            }
          }

          result.isException -> {
            val exception = result.`as`<AbstractTruffleException>(AbstractTruffleException::class.java)
            ctx.sendError(
              logging,
              version,
              HttpResponseStatus.INTERNAL_SERVER_ERROR,
              "Failed to process response: ${exception.message}",
            )
          }
        }

        else -> ctx.sendError(
          logging,
          version,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Failed to process response: Unrecognized async handler result type: ${result::class.java.name}",
        )
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  override fun channelRead(channelContext: ChannelHandlerContext, message: Any) {
    // fast return
    if (message == LastHttpContent.EMPTY_LAST_CONTENT || message !is NettyHttpRequest) return
    val exec = exec.executor()

    try {
      logging.debug { "Handling HTTP request: $message" }

      // prepare the wrappers
      val handled = atomic(false)
      val doFlush = atomic(true)
      val request = Http.request(message, Http.HttpRequestOptions(
        version = ProtocolVersion.HTTP_1_1,
        host = host,
        port = port,
        scheme = scheme,
      ))
      val context = HttpContext(exec, channelContext)
      val response = NettyMutableHttpResponse.empty()

      // resolve the handler pipeline (or default to 'not found' if empty)
      router.pipeline(request, context).forEach { handler ->
        when (handler) {
          is GuestSimpleHandler -> handled.value = handler(request, response, context)
          is GuestAsyncHandler -> handler(request, response, context).also {
            handled.compareAndSet(false, true)
            doFlush.compareAndSet(true, false)

            it.addListener(Runnable {
              PolyglotResponseHandler(it.get()).send(HttpVersion.HTTP_1_1, logging, channelContext)
            }, exec)
          }
        }
      }

      if (!handled.value) {
        channelContext.writeAndFlush(
          DefaultHttpResponse(
            /* version = */ HttpVersion.HTTP_1_1,
            /* status = */ HttpResponseStatus.NOT_FOUND,
          ),
        )
        channelContext.close()
      } else if (doFlush.value) {
        logging.debug("Request processing complete, flushing response")
        channelContext.flush()
        channelContext.close()
      }
    } catch (err: Throwable) {
      logging.error { "Error handling request: ${err.stackTraceToString()}" }
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
