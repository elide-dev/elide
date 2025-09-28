/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelPromise
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import elide.runtime.intrinsics.server.http.v2.channels.ChannelScope

/**
 * An [HttpContextHandler] that can be manually controller by test code. The handler keeps track of the current request
 * context being handled and waits until [sendResponse] is called to signal completion.
 */
internal class MockContextHandler : HttpContextHandler {
  private val currentContext = AtomicReference<HttpContext>()
  private val currentPromise = AtomicReference<ChannelPromise>()

  /** Asserts that the handler is currently handling a context. */
  fun assertContextOpen(message: String = "Expected a context to be open"): HttpContext {
    return assertNotNull(currentContext.get(), message)
  }

  /** Asserts that the handler is not currently handling a context. */
  fun assertNoContext(message: String = "Expected no context to be open") {
    assertNull(currentContext.get(), message)
  }

  /** Signal completion for the current context being handled, or [fail] if no context is active. */
  fun sendResponse() {
    currentPromise.get()?.setSuccess() ?: fail("No active context being handled")
  }

  fun setFailed(cause: Throwable? = null) {
    currentPromise.get()?.setFailure(cause) ?: fail("No active context being handled")
  }

  override fun handle(context: HttpContext, scope: ChannelScope): ChannelFuture {
    val promise = scope.newPromise()

    currentContext.set(context)
    currentPromise.set(promise)

    return promise
  }
}
