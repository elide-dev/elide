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
package elide.runtime.intrinsics.server.http.netty

import io.micronaut.http.HttpHeaders
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpMethod
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.vm.annotations.Polyglot
import io.netty.handler.codec.http.HttpRequest as NettyRequest

/** [HttpRequest] implementation wrapping a Netty request object. */
@DelicateElideApi @JvmInline internal value class NettyHttpRequest(private val request: NettyRequest) : HttpRequest {
  // Resolve the charset to use when decoding request body content.
  private fun charsetFor(request: NettyRequest): Charset {
    val charsetHeader = request.headers().get(HttpHeaders.CONTENT_TYPE)
    if (charsetHeader != null) {
      val parts = charsetHeader.split(";")
      for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.startsWith("charset=", ignoreCase = true)) {
          val charsetName = trimmed.substringAfter('=').trim()
          return try {
            Charset.forName(charsetName)
          } catch (_: RuntimeException) {
            error("Invalid charset name")
          }
        }
      }
    }

    // default: utf-8
    return StandardCharsets.UTF_8
  }

  @get:Polyglot override val uri: String get() = request.uri()
  @get:Polyglot override val method: HttpMethod get() = HttpMethod.valueOf(request.method().name())
  @get:Polyglot override val version: String get() = request.protocolVersion().toString()
  @get:Polyglot override val body: String? get() = when (val req = request) {
    // if we have a body, decode it according to request charset
    is FullHttpRequest -> req.content().toString(charsetFor(request))

    // otherwise, if we don't have a body value, this is null
    is DefaultHttpRequest -> null

    // unrecognized request types hard-fail
    else -> error("Unexpected HTTP request type: $request")
  }
}
