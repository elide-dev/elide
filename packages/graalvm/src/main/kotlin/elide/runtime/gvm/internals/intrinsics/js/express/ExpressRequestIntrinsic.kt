package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.gvm.internals.intrinsics.js.JsProxy
import org.graalvm.polyglot.proxy.ProxyObject
import reactor.netty.http.server.HttpServerRequest
import elide.runtime.intrinsics.js.express.ExpressRequest

/** An intrinsic helper used to construct JavaScript request objects around a Reactor Netty [HttpServerRequest]. */
internal object ExpressRequestIntrinsic {
  fun from(request: HttpServerRequest): ProxyObject = JsProxy.build {
    // members extracted from the request by Reactor Netty
    put("path", request.path())
    put("url", request.uri())
    put("method", request.method().name())

    // request parameters are populated by the routing code
    putObject("params")
  }
}
