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
package elide.runtime.http.server.js.node

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import elide.runtime.exec.ContextAware
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.http.server.*

/**
 * An adapter used to bind an HTTP server as part of Elide's Node.js `http` module implementation.
 * Subclasses implement the [dispatch] method and schedule execution using a request/response pair.
 */
public abstract class NodeHttpServerApplication(
  private val executor: ContextAwareExecutor
) : HttpApplication<CallContext.Empty> {

  @ContextAware protected abstract fun dispatch(request: NodeHttpServerRequest, response: NodeHttpServerResponse)

  override fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: HttpRequestBody,
    responseBody: HttpResponseBody
  ): CallContext.Empty = CallContext.Empty

  override fun handle(call: HttpCall<CallContext.Empty>) {
    executor.execute {
      val request = NodeHttpServerRequest(call, executor)
      val response = NodeHttpServerResponse(call, request, executor)

      runCatching { dispatch(request, response) }
        .onFailure { call.fail(it) }
    }
  }
}
