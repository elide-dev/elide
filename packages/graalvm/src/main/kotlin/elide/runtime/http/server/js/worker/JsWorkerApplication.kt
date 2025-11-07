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
package elide.runtime.http.server.js.worker

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchHeadersIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchRequestIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.http.server.*
import elide.runtime.http.server.netty.*
import elide.runtime.intrinsics.js.FetchResponse

/**
 * An adapter used to serve Workers-style JavaScript/TypeScript applications on Elide's HTTP server engine.
 *
 * Applications are defined as a function that accepts a request and an env object, and returns a response or a promise
 * that resolves to a response. The engine maps the response content and headers automatically before sending the call
 * once the handler function returns.
 *
 * @see JsWorkerEntrypoint
 */
public class JsWorkerApplication(
  private val source: Source,
  private val executor: ContextAwareExecutor,
) : HttpApplication<JsWorkerEnv> {
  /** A JS entrypoint function resolved for each context. */
  private val localHandler = ContextLocal<JsWorkerEntrypoint>()

  /** Lazily resolved host info. */
  @Volatile private var hostInfo: HttpApplicationStack.ServiceBinding? = null

  override fun onStart(stack: HttpApplicationStack) {
    // prefer the address to the HTTPS service, fall back to HTTP/3,
    // and finally use cleartext if nothing else is running
    val services = stack.services.associateBy { it.label }

    val binding = services[HttpsService.LABEL]?.bindResult?.getOrNull()
      ?: services[Http3Service.LABEL]?.bindResult?.getOrNull()
      ?: services[HttpCleartextService.LABEL]?.bindResult?.getOrNull()
      ?: error("Unable to resolve bound host address: no services running")

    hostInfo = binding
  }

  override fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: ReadableContentStream,
    responseBody: WritableContentStream
  ): JsWorkerEnv {
    val host = hostInfo ?: error("Host info is unresolved, cannot accept calls before binding")
    return JsWorkerEnv(
      scheme = host.scheme,
      host = host.address.hostnameOrDomainPath() ?: JsWorkerEnv.DEFAULT_HOST,
      port = host.address.portOrNull() ?: JsWorkerEnv.DEFAULT_PORT,
    )
  }

  override fun handle(call: HttpCall<JsWorkerEnv>) {
    executor.execute {
      val context = Context.getCurrent()
      val fetch = localHandler.current() ?: JsWorkerEntrypoint.resolve(source, context)
        .also { executor.setContextLocal(localHandler, it) }

      val host = hostInfo ?: error("Host info is unresolved, cannot accept calls before binding")

      val fetchHeaders = FetchHeadersIntrinsic().apply {
        call.request.headers().forEach { append(it.key, it.value) }
      }

      val fetchRequest = FetchRequestIntrinsic(
        targetUrl = URLIntrinsic.URLValue.fromString("${host.assembleUri()}${call.request.uri()}"),
        targetMethod = call.request.method().name(),
        requestHeaders = fetchHeaders,
        bodyData = JsWorkerStreams.forRequest(call.requestBody, executor),
      )

      fetch(fetchRequest, call.context).then(
        onCatch = { call.fail(IllegalStateException("Request handler failed: $it")) },
        onFulfilled = { fetchResponse ->
          applyResponse(fetchResponse, call.response)
          fetchResponse.body?.let { JsWorkerStreams.consumeResponseStream(it, call.responseBody, executor) }
          call.send()
        },
      )
    }
  }

  private fun applyResponse(fetchResponse: FetchResponse, callResponse: HttpResponse) {
    callResponse.status = HttpResponseStatus.valueOf(fetchResponse.status, fetchResponse.statusText)
    fetchResponse.headers.forEach { callResponse.addHeader(it.key, it.value) }
  }
}
