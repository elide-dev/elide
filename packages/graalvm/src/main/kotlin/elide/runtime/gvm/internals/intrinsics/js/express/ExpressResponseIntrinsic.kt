package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.intrinsics.js.express.ExpressResponse
import io.micronaut.core.async.publisher.Publishers
import org.graalvm.polyglot.Value
import reactor.netty.http.server.HttpServerResponse

/** An [ExpressResponse] implemented as a wrapper around a Reactor Netty [HttpServerResponse]. */
internal class ExpressResponseIntrinsic(private val response: HttpServerResponse) : ExpressResponse {
  override fun send(body: Value) {
    when {
      // TODO: support JSON objects and other types of content
      // treat any other type of value as a string (or force conversion)
      else -> response.sendString(Publishers.just(body.toString()))
    }
  }
}