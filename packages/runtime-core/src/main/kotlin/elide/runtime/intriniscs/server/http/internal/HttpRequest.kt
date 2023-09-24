package elide.runtime.intriniscs.server.http.internal

import io.netty.handler.codec.http.HttpMethod
import elide.runtime.core.DelicateElideApi
import io.netty.handler.codec.http.HttpRequest as NettyRequest

/** A lightweight wrapper around a Netty HTTP request, allowing guest code to access request information. */
@DelicateElideApi @JvmInline internal value class HttpRequest(private val request: NettyRequest) {
  /** The URI (path) for this request. */
  val uri: String get() = request.uri()

  /** The HTTP method for this request */
  val method: HttpMethod get() = request.method()
}