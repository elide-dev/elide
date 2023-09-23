package elide.runtime.intriniscs.server.http.internal

import io.netty.handler.codec.http.HttpMethod
import elide.runtime.core.DelicateElideApi
import io.netty.handler.codec.http.HttpRequest as NettyRequest

@DelicateElideApi @JvmInline internal value class HttpRequest(private val request: NettyRequest) {
  val uri: String get() = request.uri()
  val method: HttpMethod get() = request.method()
}