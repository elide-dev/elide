package elide.runtime.intriniscs.server.http.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

@DelicateElideApi internal class HttpResponse(private val context: ChannelHandlerContext) {
  private val headers = DefaultHttpHeaders(false)

  /** Send part of the request content, automatically flushing the headers if necessary. */
  private fun sendBody(status: HttpResponseStatus, content: ByteBuf?) {
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

  fun send(status: HttpResponseStatus) {
    sendBody(status, null)
  }

  private fun send(status: HttpResponseStatus, body: PolyglotValue) {
    // TODO: support JSON objects and other types of content
    // treat any type of value as a string (force conversion)
    sendBody(status, Unpooled.wrappedBuffer(body.toString().toByteArray()))
  }

  @Export public fun send(status: Int, body: PolyglotValue?) {
    if (body != null) {
      send(HttpResponseStatus.valueOf(status), body)
    } else {
      send(HttpResponseStatus.valueOf(status))
    }
  }
}