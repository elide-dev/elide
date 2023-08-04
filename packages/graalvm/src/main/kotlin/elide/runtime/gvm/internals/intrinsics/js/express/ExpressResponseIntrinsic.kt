/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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
