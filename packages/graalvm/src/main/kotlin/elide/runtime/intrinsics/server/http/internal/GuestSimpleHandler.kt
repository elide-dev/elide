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
package elide.runtime.intrinsics.server.http.internal

import elide.http.MutableResponse
import elide.http.Request
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse

/**
 * Lightweight wrapper around a [PolyglotValue] that represents an executable request handler, allowing calls to the
 * underlying guest callable as a normal Kotlin [function][GuestHandlerFunction].
 *
 * Note that using [GuestHandler.of] to wrap a value will verify that it can be executed, but signature checks are not
 * available, meaning that function-like values that don't match a handler's signature are technically possible.
 */
@DelicateElideApi @JvmInline internal value class GuestSimpleHandler internal constructor(
  private val value: PolyglotValue
) : GuestHandler, GuestHandlerFunction<Boolean> {
  override fun invoke(request: Request, response: MutableResponse, context: HttpContext): Boolean {
    HttpRequest.of(request).let { wrapped ->
      val responder = HttpResponse.of(response, context.channelContext)
      return value.execute(wrapped, responder, context).let { result ->
        when {
          result.isBoolean -> result.asBoolean()
          // default to handled to avoid 404 fallthrough
          else -> true
        }
      }
    }
  }
  
  internal companion object {
    /** Wraps a [PolyglotValue] and returns it as a [GuestHandler]. The [value] must be executable. */
    infix fun of(value: PolyglotValue): GuestHandler {
      // we can only verify whether the value is function-like
      require(value.canExecute()) { "Guest handlers must be executable values." }
      return GuestSimpleHandler(value)
    }
  }
}
