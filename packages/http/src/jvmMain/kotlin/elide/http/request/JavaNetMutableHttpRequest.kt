/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.http.request

import io.netty.buffer.ByteBufInputStream
import java.net.http.HttpRequest
import java.util.function.BiPredicate
import elide.http.Body
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Request
import elide.http.alwaysTruePredicate
import elide.http.body.NettyBody
import elide.http.body.PublisherBody
import elide.http.headers.JavaNetMutableHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP request type for `java.net.http` with mutability.
internal class JavaNetMutableHttpRequest private constructor (
  private val builder: HttpRequest.Builder,
  @Volatile private var backing: HttpRequest,
): PlatformMutableHttpRequest<HttpRequest> {
  override val request: HttpRequest get() = backing
  override val version: ProtocolVersion get() = backing.toProtocolVersion()
  override val method: Method get() = Method.of(backing.method())
  override val headers: MutableHeaders = JavaNetMutableHttpHeaders(backing.headers())
  override fun build(): Request = JavaNetHttpRequest(backing)

  override var url: HttpUrl
    get() = JavaNetHttpUri(backing.uri())
    set(value) = when (val uri = value) {
      is JavaNetHttpUri -> {
        builder.uri(uri.value)
        backing = builder.build()
      }
      else -> error("Unsupported URI type: $value")
    }

  override var body: Body
    get() = backing.bodyPublisher().let { pub ->
      when {
        pub == null || pub.isEmpty -> Body.Empty
        else -> PublisherBody(pub.get())
      }
    }
    set(value) {
      when (value) {
        is Body.Empty -> backing = builder.apply {
          method(method.symbol, HttpRequest.BodyPublishers.noBody())
        }.build()

        is PublisherBody -> backing = builder.apply {
          method(method.symbol, value.unwrap())
        }.build()

        is NettyBody -> builder.apply {
          method(method.symbol, HttpRequest.BodyPublishers.ofInputStream {
            ByteBufInputStream(value.unwrap())
          })
        }.build()

        else -> error("Unsupported body type: $value")
      }
    }

  /** Factories for building or obtaining a [JavaNetMutableHttpRequest]. */
  companion object {
    /** @return New mutable HTTP request from the given [HttpRequest]. */
    @JvmStatic fun of(req: HttpRequest): JavaNetMutableHttpRequest = JavaNetMutableHttpRequest(
      HttpRequest.newBuilder(req, alwaysTruePredicate),
      req,
    )
  }
}
