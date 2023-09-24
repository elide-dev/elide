package elide.runtime.intriniscs.server.http.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/** A lightweight wrapper around a Netty HTTP respond, allowing guest code to modify and send the response. */
@DelicateElideApi internal class HttpResponse(private val context: ChannelHandlerContext) {
  /** Headers for this response, dispatched once the response is sent to the client. */
  private val headers = DefaultHttpHeaders(false)

  /**
   * Exported method allowing guest code to send the response to the client. This overload resolves a status code and
   * wraps the body as needed before sending.
   */
  @Export fun send(status: Int, body: PolyglotValue?) {
    if (body != null) {
      send(HttpResponseStatus.valueOf(status), body)
    } else {
      send(HttpResponseStatus.valueOf(status))
    }
  }

  /**
   * Send part of the request content, automatically flushing the headers if necessary.
   *
   * @param status The HTTP status code for the response.
   * @param content The raw content buffer for this response.
   */
  internal fun sendBody(status: HttpResponseStatus, content: ByteBuf?) {
    headers
      .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
      .set(HttpHeaderNames.SERVER, "Elide")

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

    context.write(response)
  }

  /** Send an empty response, setting only the status code. */
  internal fun send(status: HttpResponseStatus) {
    sendBody(status, null)
  }

  /**
   * Sends a response with the provided [status] and [body], allocating a buffer for the content as necessary.
   *
   * @param status The HTTP status code of the response.
   * @param body A guest value to be unwrapped and used as response content.
   */
  private fun send(status: HttpResponseStatus, body: PolyglotValue) {
    // TODO: support JSON objects and other types of content
    // treat any type of value as a string (force conversion)
    sendBody(status, Unpooled.wrappedBuffer(body.toString().toByteArray()))
  }
}