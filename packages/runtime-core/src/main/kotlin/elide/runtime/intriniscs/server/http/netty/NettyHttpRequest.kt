package elide.runtime.intriniscs.server.http.netty

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intriniscs.server.http.HttpMethod
import elide.runtime.intriniscs.server.http.HttpRequest
import io.netty.handler.codec.http.HttpRequest as NettyRequest

/** [HttpRequest] implementation wrapping a Netty request object. */
@DelicateElideApi @JvmInline internal value class NettyHttpRequest(private val request: NettyRequest) : HttpRequest {
  @get:Export override val uri: String get() = request.uri()
  @get:Export override val method: HttpMethod get() = HttpMethod.valueOf(request.method().name())
}