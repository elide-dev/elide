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

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** A basic [HttpContext] that can be used in tests. Use the [Factory] class to create and track test instances. */
internal class MockHttpContext(
  override val request: HttpRequest,
  override val requestBody: HttpContentSource,
  override val response: HttpResponse,
  override val responseBody: HttpContentSink,
  override val session: HttpSession,
  private val completion: ChannelPromise,
) : HttpContext {
  override fun close() {
    completion.setSuccess()
  }

  /**
   * A factory for [MockHttpContext] instances that keeps track of the last created context. Useful during tests when
   * checking whether a context is created or not is required.
   *
   * If the test can control which [HttpContextHandler] to use, prefer using [MockContextHandler] for tracking the
   * active context instead of this class.
   */
  class Factory : HttpContextFactory<MockHttpContext> {
    private val currentContext = AtomicReference<MockHttpContext>()

    override fun newContext(
      incomingRequest: HttpRequest,
      channelContext: ChannelHandlerContext,
      requestSource: HttpContentSource,
      responseSink: HttpContentSink
    ): MockHttpContext {
      val completion = channelContext.newPromise()
      val context = MockHttpContext(
        request = incomingRequest,
        requestBody = requestSource,
        response = DefaultHttpResponse(incomingRequest.protocolVersion(), HttpResponseStatus.OK),
        responseBody = responseSink,
        session = HttpSession(),
        completion = completion,
      )

      completion.addListener { currentContext.compareAndSet(context, null) }
      currentContext.set(context)
      return context
    }

    fun assertContextOpen(message: String = "Expected a context to be open"): MockHttpContext {
      return assertNotNull(currentContext.get(), message)
    }

    fun assertNoContext(message: String = "Expected no context to be open") {
      assertNull(currentContext.get(), message)
    }
  }
}
