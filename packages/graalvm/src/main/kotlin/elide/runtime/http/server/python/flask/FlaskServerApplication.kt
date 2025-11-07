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
package elide.runtime.http.server.python.flask

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import java.nio.file.Path
import kotlinx.serialization.json.Json
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.gvm.internals.serialization.GuestValueSerializer
import elide.runtime.http.server.*
import elide.runtime.intrinsics.python.flask.FlaskRequestAccessor
import elide.runtime.intrinsics.python.flask.FlaskResponseObject

public class FlaskServerApplication internal constructor(
  private val entrypoint: Source,
  private val applicationRoot: Path?,
  private val executor: ContextAwareExecutor,
  private val requestAccessor: FlaskRequestAccessor,
  private val localRouter: ContextLocal<FlaskRouter>,
) : HttpApplication<FlaskContext> {
  override fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: HttpRequestBody,
    responseBody: HttpResponseBody
  ): FlaskContext = FlaskContext(request)

  override fun handle(call: HttpCall<FlaskContext>) {
    // early for static asset requests
    applicationRoot?.let { root ->
      if (FlaskStaticAssetsRouter.serveStaticAsset(root, call)) {
        call.send()
        return
      }
    }

    // resolve thread-local or shared routing stack
    executor.execute {
      val stack = localRouter.current() ?: FlaskRouter().also {
        executor.setContextLocal(localRouter, it)

        // initialize the stack for this context
        Context.getCurrent().eval(entrypoint)
      }

      runCatching {
        // map context to arguments expected by guest code here
        when (val match = stack.match(call.request)) {
          FlaskRouter.MatcherResult.NoMatch -> call.setEmptyResponse(HttpResponseStatus.NOT_FOUND)
          FlaskRouter.MatcherResult.MethodNotAllowed -> call.setEmptyResponse(HttpResponseStatus.METHOD_NOT_ALLOWED)
          is FlaskRouter.MatcherResult.MissingVariable,
          is FlaskRouter.MatcherResult.InvalidVariable -> call.setEmptyResponse(HttpResponseStatus.BAD_REQUEST)

          is FlaskRouter.MatcherResult.Match -> {
            requestAccessor.push(call)

            try {
              @Suppress("UNCHECKED_CAST")
              val result = match.handler.execute(ProxyHashMap.from(match.parameters as Map<Any, Any>))
              applyHandlerResponse(result, call)
            } catch (e: PolyglotException) {
              if (!e.isHostException) throw e
              val cause = e.asHostException() as? FlaskHttpException ?: throw e

              call.response.status = HttpResponseStatus.valueOf(cause.code)
              call.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
              call.responseBody.close()
            } finally {
              requestAccessor.pop()
            }
          }
        }
      }.fold(
        onSuccess = { call.send() },
        onFailure = { call.fail(it) },
      )
    }
  }

  private fun HttpCall<FlaskContext>.setEmptyResponse(status: HttpResponseStatus) {
    response.status = status
    response.setHeader(HttpHeaderNames.CONTENT_LENGTH, 0)
  }

  private fun HttpCall<FlaskContext>.useStringContent(overrideStatus: Boolean, value: Value) {
    val content = Unpooled.copiedBuffer(value.asString(), Charsets.UTF_8)
    response.headers().apply {
      set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex().toString())
      set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
    }

    if (overrideStatus) response.status = HttpResponseStatus.OK
    responseBody.source(content)
  }

  private fun HttpCall<FlaskContext>.useIteratorResponse(value: Value) {
    response.headers().apply {
      set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
      set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
    }

    responseBody.source {
      if (!value.hasIteratorNextElement()) it.end()
      else it.write(mapContentChunk(value.iteratorNextElement))
    }
  }

  private fun HttpCall<FlaskContext>.useListResponse(overrideStatus: Boolean, value: Value) {
    val content = Unpooled.copiedBuffer(encodeValueAsJSON(value), Charsets.UTF_8)

    if (overrideStatus) response.status = HttpResponseStatus.OK
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex())
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")

    responseBody.source(content)
  }

  private fun HttpCall<FlaskContext>.applyResponseTuple(overrideStatus: Boolean, value: Value) {
    val status: HttpResponseStatus
    val headers: Value?

    when (value.arraySize) {
      2L -> value.getArrayElement(1).let { it ->
        when {
          it.isNumber -> {
            status = it.takeIf { code -> code.isNumber && code.fitsInInt() }
              ?.let { code -> HttpResponseStatus.valueOf(code.asInt()) }
              ?: error("Invalid status code in response tuple: ${value.getArrayElement(1)}")

            headers = null
          }

          it.hasHashEntries() || it.hasArrayElements() -> {
            headers = it
            status = HttpResponseStatus.OK
          }

          else -> error("Invalid response tuple: expected status or headers, got $it")
        }
      }

      3L -> {
        status = value.getArrayElement(1).takeIf { it.isNumber && it.fitsInInt() }
          ?.let { HttpResponseStatus.valueOf(it.asInt()) }
          ?: error("Invalid status code in response tuple: ${value.getArrayElement(1)}")

        headers = value.getArrayElement(2)
      }

      // invalid tuple
      else -> error("Invalid response tuple: expected 2 or 3 elements, got ${value.arraySize}")
    }

    if (overrideStatus) response.status = status

    if (headers != null) when {
      headers.hasHashEntries() -> {
        val iterator = headers.hashEntriesIterator
        while (iterator.hasIteratorNextElement()) iterator.iteratorNextElement.let {
          response.headers().add(
            /* name = */ it.getArrayElement(0).asString(),
            /* value = */ it.getArrayElement(1).toString(),
          )
        }
      }

      else -> error("Invalid headers provided to response tuple: expected a dict, got $headers")
    }

    // the first element is always the response itself
    applyHandlerResponse(value.getArrayElement(0), this, overrideStatus = false)
  }

  private fun HttpCall<FlaskContext>.useHostResponse(overrideStatus: Boolean, returnValue: Value) {
    // it could be a content producer
    val producer = runCatching { returnValue.asHostObject<HttpResponseSource>() }
      .getOrNull()
      ?: error("Invalid Flask response object provided: $returnValue")

    if (overrideStatus) response.status = HttpResponseStatus.OK
    response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
    responseBody.source(producer)
  }

  private fun HttpCall<FlaskContext>.useFlaskResponse(returnValue: Value) {
    val responseObject = runCatching { returnValue.asProxyObject<FlaskResponseObject>() }
      .getOrNull()
      ?: error("Invalid Flask response object provided: $returnValue")

    response.status = responseObject.status
    responseObject.headers.forEach { (name, value) -> response.headers().add(name, value) }
    applyHandlerResponse(responseObject.content, this, overrideStatus = false)
  }

  private fun applyHandlerResponse(
    returnValue: Value,
    call: HttpCall<FlaskContext>,
    overrideStatus: Boolean = true,
  ) {
    when {
      returnValue.isHostObject -> call.useHostResponse(overrideStatus, returnValue)
      returnValue.isProxyObject -> call.useFlaskResponse(returnValue)
      returnValue.isString -> call.useStringContent(overrideStatus, returnValue)
      returnValue.isIterator -> call.useIteratorResponse(returnValue)
      returnValue.hasArrayElements() && returnValue.metaObject?.metaSimpleName == "tuple" -> {
        call.applyResponseTuple(overrideStatus, returnValue)
      }

      (returnValue.hasHashEntries() || (returnValue.hasArrayElements()) &&
              returnValue.metaObject?.metaSimpleName == "list") -> {
        call.useListResponse(overrideStatus, returnValue)
      }

      else -> {
        // use defaults
        if (overrideStatus) call.response.status = HttpResponseStatus.OK
        call.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
      }
    }
  }

  private fun encodeValueAsJSON(value: Value): String {
    return FlaskJson.encodeToString(GuestValueSerializer, value)
  }

  private fun mapContentChunk(value: Value): ByteBuf = when {
    // bytes object
    value.hasBufferElements() -> {
      val bytes = ByteArray(value.bufferSize.toInt())
      value.readBuffer(0L, bytes, 0, bytes.size)

      Unpooled.wrappedBuffer(bytes)
    }
    // string content
    value.isString -> Unpooled.copiedBuffer(value.asString(), Charsets.UTF_8)
    else -> error("Value type is not supported as response body: $value")
  }

  private companion object {
    private val FlaskJson by lazy { Json }
  }
}
