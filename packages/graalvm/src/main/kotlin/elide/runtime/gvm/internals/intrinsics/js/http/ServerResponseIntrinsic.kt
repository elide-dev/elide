package elide.runtime.gvm.internals.intrinsics.js.http

import elide.runtime.intrinsics.js.err.Error
import elide.runtime.intrinsics.js.http.HttpHeaders
import elide.runtime.intrinsics.js.http.IncomingMessage
import elide.runtime.intrinsics.js.http.OutgoingMessage
import elide.runtime.intrinsics.js.http.ServerResponse
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Value
import io.netty.handler.codec.http.HttpHeaders as NettyHttpHeaders

internal open class ServerResponseIntrinsic(
  override val req: IncomingMessage,
  private val context: ChannelHandlerContext,
  private val httpVersion: HttpVersion,
  private val headers: NettyHttpHeaders = DefaultHttpHeaders(),
  private val trailers: NettyHttpHeaders = DefaultHttpHeaders(),
) : ServerResponse {
  /** Default status code for the response. */
  private var httpStatus: HttpResponseStatus = HttpResponseStatus.OK

  override var headersSent: Boolean = false

  override var statusCode: Int
    get() = httpStatus.code()
    set(value) {
      httpStatus = HttpResponseStatus.valueOf(value)
    }

  override var statusMessage: String
    get() = httpStatus.reasonPhrase()
    set(value) {
      httpStatus = HttpResponseStatus(statusCode, value)
    }

  override fun addTrailers(headers: HttpHeaders) = headers.forEach {
    trailers.add(it.key, it.value)
  }

  override fun appendHeader(name: String, value: String): OutgoingMessage = apply {
    headers.add(name, value)
  }

  override fun flushHeaders() {
    context.writeAndFlush(DefaultHttpResponse(httpVersion, httpStatus, headers))
    headersSent = true
  }

  override fun getHeader(name: String): String? {
    return headers.get(name)
  }

  override fun getHeaderNames(): List<String> {
    return headers.map { it.key }
  }

  override fun getHeaders(): HttpHeaders {
    return HttpHeadersIntrinsic.from(headers)
  }

  override fun hasHeader(name: String): Boolean {
    return headers.contains(name)
  }

  override fun removeHeader(name: String) {
    headers.remove(name)
  }

  override fun setHeader(name: String, value: Any): OutgoingMessage = apply {
    headers.set(name, value)
  }

  override fun setHeaders(headers: HttpHeaders): OutgoingMessage = apply {
    headers.forEach { this.headers.set(it.key, it.value) }
  }

  override fun write(chunk: Value, encoding: String?, callback: (() -> Unit)?): Boolean {
    if (!headersSent) flushHeaders()

    context.write(DefaultHttpContent(contentBuffer(chunk)))
    return true
  }

  override fun end(chunk: Value?, encoding: String?, callback: (() -> Unit)?): OutgoingMessage = apply {
    val future = if (headersSent) context.writeAndFlush(
      DefaultLastHttpContent(
        /* content = */ contentBuffer(chunk),
        /* trailingHeaders = */ trailers,
      )
    ) else context.writeAndFlush(
      DefaultFullHttpResponse(
        /* version = */ httpVersion,
        /* status = */ httpStatus,
        /* content = */ contentBuffer(chunk),
        /* headers = */ headers,
        /* trailingHeaders = */ trailers
      )
    )

    // invoke the callback if specified
    callback?.let { future.addListener { it() } }
  }

  override fun destroy(error: Error?): OutgoingMessage = apply {
    context.close()
  }

  private fun contentBuffer(content: Value?): ByteBuf = when {
    // null content can be sent as an empty buffer, the encoder will skip the chunk
    content == null -> Unpooled.EMPTY_BUFFER

    // string content can be wrapped in a buffer with the default charset
    content.isString -> Unpooled.copiedBuffer(content.asString(), Charsets.UTF_8)

    // Buffer and UInt8Array instances are also accepted, and will be read one bye at a time, note that Buffer is a
    // subclass of UInt8Array, which allows using the same code for both cases
    content.isMetaObject && (content.metaQualifiedName == "Buffer" || content.metaQualifiedName == "UInt8Array") -> {
      val buffer = Unpooled.buffer(content.arraySize.toInt())
      for (i in 0 until content.arraySize) {
        buffer.writeByte(content.readBufferByte(i).toInt())
      }

      buffer
    }

    else -> error("Unsupported content type: ${content.metaQualifiedName}")
  }
}