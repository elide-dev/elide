package elide.runtime.gvm.internals.intrinsics.js.http

import elide.runtime.intrinsics.js.http.HttpHeaders
import elide.runtime.intrinsics.js.http.IncomingMessage
import io.netty.handler.codec.DecoderResult
import io.netty.handler.codec.http.FullHttpRequest

internal class IncomingMessageIntrinsic(private val request: FullHttpRequest) : IncomingMessage {
  override val url: String
    get() = request.uri()

  override val complete: Boolean
    get() = request.decoderResult() != DecoderResult.UNFINISHED

  override val httpVersion: String
    get() = request.protocolVersion().run { "${majorVersion()}.${minorVersion()}" }

  override val method: String
    get() = request.method().name()

  override val headers: HttpHeaders  = HttpHeadersIntrinsic.from(request.headers())

  override val headersDistinct: HttpHeaders = headers

  override val rawHeaders: List<String> = buildList {
    request.headers().forEach {
      add(it.key)
      add(it.value)
    }
  }

  override val trailers: HttpHeaders = HttpHeadersIntrinsic.from(request.trailingHeaders())

  override val trailersDistinct: HttpHeaders = trailers

  override val rawTrailers: List<String> = buildList {
    request.trailingHeaders().forEach {
      add(it.key)
      add(it.value)
    }
  }

  override val statusCode: Int
    get() = error("Status code is not available for incoming requests")

  override val statusMessage: String
    get() = error("Status message is not available for incoming requests")
}