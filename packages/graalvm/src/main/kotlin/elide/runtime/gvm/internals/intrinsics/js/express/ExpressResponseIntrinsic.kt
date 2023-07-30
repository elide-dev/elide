package elide.runtime.gvm.internals.intrinsics.js.express

import io.micronaut.core.async.publisher.Publishers
import org.graalvm.polyglot.Value
import org.reactivestreams.Publisher
import reactor.netty.NettyOutbound
import reactor.netty.http.server.HttpServerResponse
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.js.express.ExpressResponse

/** An [ExpressResponse] implemented as a wrapper around a Reactor Netty [HttpServerResponse]. */
internal class ExpressResponseIntrinsic(
  private val response: HttpServerResponse
) : ExpressResponse {
  private val publisher = AtomicReference<NettyOutbound>()

  /** Finish processing this response and return a [Publisher] to be returned from the outer handler */
  fun end(): Publisher<Void> = publisher.get() ?: response.send()

  private fun publish(next: NettyOutbound) {
    publisher.getAndUpdate { current -> current?.then(next) ?: next }
  }

  override fun send(body: Value) = publish(
    when {
      // TODO: support JSON objects and other types of content
      // treat any other type of value as a string (or force conversion)
      else -> response.sendString(Publishers.just(body.toString()))
    }
  )
}
