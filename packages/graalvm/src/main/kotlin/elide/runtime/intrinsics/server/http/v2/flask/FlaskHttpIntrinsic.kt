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
package elide.runtime.intrinsics.server.http.v2.flask

import com.oracle.truffle.api.CompilerDirectives
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import jakarta.inject.Provider
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import elide.annotations.Singleton
import elide.runtime.Logging
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeLatch
import elide.runtime.core.SharedContextFactory
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.internals.serialization.GuestValueSerializer
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.server.http.v2.*
import elide.runtime.intrinsics.server.http.v2.flask.FlaskStaticAssetsRouter.serveStaticAsset
import elide.runtime.plugins.python.flask.FlaskAPI
import elide.runtime.gvm.GuestLanguage as GVMGuestLanguage

private const val ROUTE_METHOD = "route"
private const val BIND_METHOD = "bind"

@Singleton public class FlaskHttpIntrinsic(
  private val runtimeLatchProvider: Provider<RuntimeLatch>,
  entrypointProvider: Provider<EntrypointRegistry>,
  contextProvider: Provider<SharedContextFactory>,
) : AbstractHttpIntrinsic(), ProxyExecutable, ProxyObject, GuestIntrinsic, FlaskAPI {
  override val runtimeLatch: RuntimeLatch by lazy { runtimeLatchProvider.get() }
  private val applicationRoot by lazy { entrypointProvider.get()?.acquire()?.path?.let { Path(it) }?.parent }

  private val requestAccessor = FlaskRequestAccessor()

  public inner class FlaskAppInstance(internal val name: String) : ProxyObject {
    init {
      // automatically bind when the runtime is ready to wait for long tasks
      runtimeLatch.onAwait { bind(3000) }
    }

    override fun getMember(key: String?): Any? = when (key) {
      ROUTE_METHOD -> ProxyExecutable { args /* (name, rule, methods, handler) */ ->
        val endpoint = args.getOrNull(0)?.takeIf { it.isString }
          ?: error("Invalid route; pass a string to @app.route")
        val pattern = args.getOrNull(1)?.takeIf { it.isString }
          ?: error("Expected route pattern; check guest wrapper")
        val methods = args.getOrNull(2)?.takeIf { it.hasArrayElements() }
          ?: error("Expected methods; check guest wrapper")
        val handler = requireNotNull(args.last()) { "no handler provided; check guest wrapper" }
        require(handler.canExecute()) { "guest python handler is not executable" }

        stackManager.withStack {
          val mappedMethods = Array(methods.arraySize.toInt()) { method ->
            HttpMethod.valueOf(methods.getArrayElement(method.toLong()).asString())
          }
          it.register(endpoint.asString(), pattern.asString(), mappedMethods, handler)
        }
      }

      BIND_METHOD -> ProxyExecutable {
        bind(3000)
      }

      else -> null
    }

    override fun getMemberKeys(): Any = INSTANCE_MEMBER_KEYS
    override fun hasMember(key: String?): Boolean = key != null && key in INSTANCE_MEMBER_KEYS
    override fun putMember(key: String?, value: Value?): Unit = error("Modifying the flask app object is not allowed")
  }

  private val stackManager by lazy {
    FlaskHandlerStackManager(entrypointProvider.get(), contextProvider.get())
  }

  @CompilerDirectives.TruffleBoundary
  override fun execute(vararg arguments: Value?): FlaskAppInstance {
    // accept the application root as an argument
    val root = arguments.firstOrNull()?.takeIf { it.isString }?.asString()
      ?: error("The application package name must be specified")

    return FlaskAppInstance(root)
  }

  override fun acquireHandler(): HttpContextHandler = HttpContextHandler { httpContext, handlerScope ->
    // early for static asset requests
    applicationRoot?.let { root ->
      if (serveStaticAsset(root, httpContext as FlaskHttpContext))
        return@HttpContextHandler handlerScope.newSucceededFuture()
    }

    // resolve thread-local or shared routing stack
    stackManager.withStack { stack ->
      // map context to arguments expected by guest code here
      when (val match = stack.match(httpContext.request)) {
        FlaskRouter.MatcherResult.NoMatch -> {
          httpContext.response.status = HttpResponseStatus.NOT_FOUND
          httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
          httpContext.responseBody.close()
        }

        FlaskRouter.MatcherResult.MethodNotAllowed -> {
          httpContext.response.status = HttpResponseStatus.METHOD_NOT_ALLOWED
          httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
          httpContext.responseBody.close()
        }

        is FlaskRouter.MatcherResult.MissingVariable -> {
          httpContext.response.status = HttpResponseStatus.BAD_REQUEST
          httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
          httpContext.responseBody.close()
        }

        is FlaskRouter.MatcherResult.InvalidVariable -> {
          httpContext.response.status = HttpResponseStatus.BAD_REQUEST
          httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
          httpContext.responseBody.close()
        }

        is FlaskRouter.MatcherResult.Match -> {
          requestAccessor.push(httpContext)

          try {
            @Suppress("UNCHECKED_CAST")
            val result = match.handler.execute(ProxyHashMap.from(match.parameters as Map<Any, Any>))
            applyHandlerResponse(result, httpContext as FlaskHttpContext)
          } catch (e: PolyglotException) {
            if (!e.isHostException) throw e
            val cause = e.asHostException() as? FlaskHttpException ?: throw e

            httpContext.response.status = HttpResponseStatus.valueOf(cause.code)
            httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
            httpContext.responseBody.close()
          } finally {
            requestAccessor.pop()
          }
        }
      }

      handlerScope.newSucceededFuture()
    }
  }

  override fun acquireFactory(): HttpContextFactory<*> =
    HttpContextFactory<HttpContext> { incomingRequest, _, requestSource, responseSink ->
      FlaskHttpContext(
        request = incomingRequest,
        requestBody = requestSource,
        response = DefaultHttpResponse(incomingRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND),
        responseBody = responseSink,
        session = HttpSession(),
      )
    }

  override fun language(): GVMGuestLanguage = GraalVMGuest.PYTHON

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[FlaskAPI.FLASK_INTRINSIC.asJsSymbol()] = this
  }

  override fun symbolicName(): String = FlaskAPI.FLASK_INTRINSIC

  @Deprecated("Use symbolicName instead")
  override fun displayName(): String = FlaskAPI.FLASK_INTRINSIC
  override fun getMember(key: String?): Any? = when (key) {
    "react_template" -> ProxyExecutable { args ->
      val relativePath = args.firstOrNull()?.takeIf { it.isString }?.asString()
        ?: error("Expected a string argument for `react`")

      val scriptPath = applicationRoot?.resolve(relativePath)
        ?: error("No location info available for the current application")

      FlaskReactTemplate(scriptPath)
    }

    "request" -> requestAccessor
    "abort" -> ProxyExecutable { args ->
      val code = args.firstOrNull()?.takeIf { it.isNumber }?.asInt()
      if (code == null) logging.warn { "Invalid argument provided to `abort`: expected an int, got $code" }
      throw FlaskHttpException(code ?: 500)
    }

    "url_for" -> ProxyExecutable { args ->
      val endpoint = args.firstOrNull()?.takeIf { it.isString }?.asString()
        ?: error("Invalid argument provided to `url_for`: expected a string, got ${args.firstOrNull()}")

      val variables = args.getOrNull(1)?.takeIf { it.hasHashEntries() }?.let { hash ->
        buildMap {
          val iterator = hash.hashEntriesIterator
          while (iterator.hasIteratorNextElement()) {
            val element = iterator.iteratorNextElement
            put(element.getArrayElement(0).asString(), element.getArrayElement(1))
          }
        }
      } ?: emptyMap()

      stackManager.withStack { it.urlFor(endpoint, variables) }
    }

    "make_response" -> ProxyExecutable { args ->
      val response: FlaskResponseObject = when (args.size) {
        0 -> FlaskResponseObject()
        1 -> FlaskResponseObject(args.first())
        2 -> when {
          args[1].isNumber && args[1].fitsInInt() -> FlaskResponseObject(
            content = args.first(),
            status = HttpResponseStatus.valueOf(args[1].asInt()),
          )

          args[1].hasHashEntries() -> FlaskResponseObject(
            args.first(),
            headers = mutableMapOf<String, String>().apply {
              val entries = args[1].hashEntriesIterator
              while (entries.hasIteratorNextElement()) entries.iteratorNextElement.let {
                put(it.getArrayElement(0).asString(), it.getArrayElement(1).asString())
              }
            },
          )

          else -> error("Invalid arguments provided to `make_response`: ${args[1]} is not a valid status or headers")
        }

        3 -> FlaskResponseObject(args[0], status = HttpResponseStatus.valueOf(args[1].asInt())).apply {
          val entries = args[2].hashEntriesIterator
          while (entries.hasIteratorNextElement()) entries.iteratorNextElement.let {
            headers.put(it.getArrayElement(0).asString(), it.getArrayElement(1).asString())
          }
        }

        else -> error("Invalid arguments provided to `make_response`: expected 0 or 1 argument, got ${args.size}")
      }

      response
    }

    else -> null
  }

  override fun getMemberKeys(): Any = MODULE_MEMBER_KEYS
  override fun hasMember(key: String?): Boolean = key in MODULE_MEMBER_KEYS
  override fun putMember(key: String?, value: Value?): Unit = error("Modifying the Flask intrinsic is not allowed")

  private fun encodeValueAsJSON(value: Value): String {
    return FlaskJson.encodeToString(GuestValueSerializer, value)
  }

  private fun mapContentChunk(value: Value): HttpContent {
    return when {
      // string content
      value.isString -> {
        val content = Unpooled.copiedBuffer(value.asString(), Charsets.UTF_8)
        DefaultHttpContent(content)
      }

      // bytes object
      value.hasBufferElements() -> {
        val bytes = ByteArray(value.bufferSize.toInt())
        value.readBuffer(0L, bytes, 0, bytes.size)

        DefaultHttpContent(Unpooled.wrappedBuffer(bytes))
      }

      else -> error("Value type is not supported as response body: $value")
    }
  }

  private fun applyHandlerResponse(
    returnValue: Value,
    httpContext: FlaskHttpContext,
    overrideStatus: Boolean = true,
  ) {
    // map return value
    when {
      // simple string value, mark as plaintext and encode it into a buffer
      returnValue.isString -> {
        if (overrideStatus) httpContext.response.status = HttpResponseStatus.OK

        val content = Unpooled.copiedBuffer(returnValue.asString(), Charsets.UTF_8)
        httpContext.response.headers().apply {
          set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex().toString())
          set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
        }

        httpContext.responseBody.source(DefaultLastHttpContent(content))
      }

      // an iterator should be used for a streaming response
      returnValue.isIterator -> {
        httpContext.response.headers().apply {
          set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
          set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
        }

        httpContext.responseBody.source {
          if (!returnValue.hasIteratorNextElement()) {
            it.push(DefaultLastHttpContent(Unpooled.EMPTY_BUFFER.retainedDuplicate()))
            it.release(close = true)

            return@source
          }

          it.push(mapContentChunk(returnValue.iteratorNextElement))
        }
      }

      // a tuple can be (response, status), (response, headers), or (response, status, headers)
      returnValue.hasArrayElements() && returnValue.metaObject?.metaSimpleName == "tuple" -> {
        val status: HttpResponseStatus
        val headers: Value?

        when (returnValue.arraySize) {
          2L -> returnValue.getArrayElement(1).let {
            when {
              it.isNumber -> {
                status = it.takeIf { code -> code.isNumber && code.fitsInInt() }
                  ?.let { code -> HttpResponseStatus.valueOf(code.asInt()) }
                  ?: error("Invalid status code in response tuple: ${returnValue.getArrayElement(1)}")

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
            status = returnValue.getArrayElement(1).takeIf { it.isNumber && it.fitsInInt() }
              ?.let { HttpResponseStatus.valueOf(it.asInt()) }
              ?: error("Invalid status code in response tuple: ${returnValue.getArrayElement(1)}")

            headers = returnValue.getArrayElement(2)
          }

          // invalid tuple
          else -> error("Invalid response tuple: expected 2 or 3 elements, got ${returnValue.arraySize}")
        }

        if (overrideStatus) httpContext.response.status = status

        if (headers != null) when {
          headers.hasHashEntries() -> {
            val iterator = headers.hashEntriesIterator
            while (iterator.hasIteratorNextElement()) iterator.iteratorNextElement.let {
              httpContext.response.headers().add(
                /* name = */ it.getArrayElement(0).asString(),
                /* value = */ it.getArrayElement(1).toString(),
              )
            }
          }

          else -> error("Invalid headers provided to response tuple: expected a dict, got $headers")
        }

        // the first element is always the response itself
        applyHandlerResponse(returnValue.getArrayElement(0), httpContext, overrideStatus = false)
      }

      // a list should be serialized as JSON
      returnValue.hasHashEntries() || (returnValue.hasArrayElements() && returnValue.metaSimpleName == "list") -> {
        val content = Unpooled.copiedBuffer(encodeValueAsJSON(returnValue), Charsets.UTF_8)

        if (overrideStatus) httpContext.response.status = HttpResponseStatus.OK
        httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex())
        httpContext.response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")

        httpContext.responseBody.source(DefaultLastHttpContent(content))
      }

      returnValue.isHostObject -> {
        // it could be a content producer
        val producer = runCatching { returnValue.asHostObject<HttpContentSink.Producer>() }
          .getOrNull()
          ?: error("Invalid Flask response object provided: $returnValue")

        if (overrideStatus) httpContext.response.status = HttpResponseStatus.OK
        httpContext.response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
        httpContext.responseBody.source(producer)
      }

      returnValue.isProxyObject -> {
        // assume it is a Flask response object
        val response = runCatching { returnValue.asProxyObject<FlaskResponseObject>() }
          .getOrNull()
          ?: error("Invalid Flask response object provided: $returnValue")

        httpContext.response.status = response.status
        response.headers.forEach { (name, value) -> httpContext.response.headers().add(name, value) }
        applyHandlerResponse(response.content, httpContext, overrideStatus = false)
      }

      else -> {
        // use defaults
        if (overrideStatus) httpContext.response.status = HttpResponseStatus.OK
        httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
      }
    }
  }

  private companion object {
    private val logging by lazy { Logging.of(FlaskHttpIntrinsic::class) }
    private val FlaskJson by lazy { Json }

    private val INSTANCE_MEMBER_KEYS = arrayOf(
      BIND_METHOD,
      ROUTE_METHOD,
    )

    private val MODULE_MEMBER_KEYS = arrayOf(
      "request",
      "abort",
      "url_for",
      "make_response",
      "react_template",
    )
  }
}
