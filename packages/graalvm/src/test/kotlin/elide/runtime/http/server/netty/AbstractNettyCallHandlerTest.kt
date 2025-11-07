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
package elide.runtime.http.server.netty

import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.HttpCall
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import elide.runtime.http.server.HttpRequestBody
import elide.runtime.http.server.HttpResponseBody

abstract class AbstractNettyCallHandlerTest {
  protected class TestApplication : HttpApplication<TestContext> {
    var onHandle: ((call: HttpCall<TestContext>) -> Unit)? = null

    private var lastCall: HttpCall<TestContext>? = null
    val call: HttpCall<TestContext> get() = lastCall ?: error("No call recorded")

    override fun toString(): String = "TestApplication"
    override fun newContext(
      request: HttpRequest,
      response: HttpResponse,
      requestBody: HttpRequestBody,
      responseBody: HttpResponseBody
    ): TestContext = TestContext()

    override fun handle(call: HttpCall<TestContext>) {
      lastCall = call
      onHandle?.invoke(call)
    }
  }

  protected class TestContext : CallContext {
    var headersSent = false
      private set

    var contentFlushed = false
      private set

    var contentSent = false
      private set

    var callEnded = false
      private set

    var error: Throwable? = null
      private set

    var onNextEvent: (() -> Unit)? = null

    override fun headersSent() {
      headersSent = true
      onNextEvent?.invoke()
    }

    override fun contentFlushed() {
      contentFlushed = true
      onNextEvent?.invoke()
    }

    override fun contentSent() {
      contentSent = true
      onNextEvent?.invoke()
    }

    override fun callEnded(failure: Throwable?) {
      callEnded = true
      error = failure
      onNextEvent?.invoke()
    }
  }
}
