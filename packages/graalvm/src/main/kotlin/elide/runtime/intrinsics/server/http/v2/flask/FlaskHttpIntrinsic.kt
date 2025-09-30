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

package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import jakarta.inject.Provider
import elide.annotations.Singleton
import elide.runtime.Logging
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeLatch
import elide.runtime.core.SharedContextFactory
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.server.http.v2.*

@Singleton public class FlaskHttpIntrinsic(
  private val runtimeLatchProvider: Provider<RuntimeLatch>,
  entrypointProvider: Provider<EntrypointRegistry>,
  contextProvider: Provider<SharedContextFactory>,
) : AbstractHttpIntrinsic(), ProxyExecutable, GuestIntrinsic {
  override val runtimeLatch: RuntimeLatch get() = runtimeLatchProvider.get()
  private val requestAccessor = FlaskRequestAccessor()

  public inner class FlaskAppInstance(private val root: String) : ProxyObject {
    private fun patternParam(args: Array<Value>): String {
      return args.first().takeIf { it.isString }?.asString()
        ?: error("Expected a string argument for the routing path")
    }

    private fun registerHandler(pattern: String, methods: Array<HttpMethod>): ProxyExecutable {
      // inner decorator
      return ProxyExecutable { innerArgs ->
        // the inner decorator receives the handler function itself and returns yet another function
        val handler = innerArgs.first()
        check(handler.canExecute()) { "Expected function as route handler" }

        stackManager.withStack { it.register(pattern, methods, handler) }
        ProxyExecutable(handler::execute)
      }
    }

    override fun getMember(key: String?): Any? = when (key) {
      "route", "get" -> ProxyExecutable { args ->
        registerHandler(patternParam(args), arrayOf(HttpMethod.GET, HttpMethod.HEAD))
      }

      "post" -> ProxyExecutable { args ->
        registerHandler(patternParam(args), arrayOf(HttpMethod.POST))
      }

      "put" -> ProxyExecutable { args ->
        registerHandler(patternParam(args), arrayOf(HttpMethod.PUT))
      }

      "patch" -> ProxyExecutable { args ->
        registerHandler(patternParam(args), arrayOf(HttpMethod.PATCH))
      }

      "delete" -> ProxyExecutable { args ->
        registerHandler(patternParam(args), arrayOf(HttpMethod.DELETE))
      }

      "bind" -> ProxyExecutable {
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

  override fun execute(vararg arguments: Value?): FlaskAppInstance {
    // accept the application root as an argument
    val root = arguments.firstOrNull()?.takeIf { it.isString }?.asString()
      ?: error("The application package name must be specified")

    return FlaskAppInstance(root)
  }

  override fun acquireHandler(): HttpContextHandler = HttpContextHandler { httpContext, handlerScope ->
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
          // TODO: add matched path variables to the context
          requestAccessor.push(httpContext)

          val result = try {
            match.handler.execute()
          } finally {
            requestAccessor.pop()
          }

          // map return value here (e.g., use a String return value to create the response body)
          // ...

          // if the guest handler is async, schedule for execution and return a handle
          // otherwise finalize handling and send the response
          val content = Unpooled.copiedBuffer(result.asString(), Charsets.UTF_8)
          httpContext.response.headers().apply {
            set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex().toString())
            set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
          }

          httpContext.response.status = HttpResponseStatus.OK
          httpContext.responseBody.source(
            object : HttpContentSink.Producer {
              override fun pull(handle: HttpContentSink.Handle) {
                handle.push(DefaultLastHttpContent(content))
                handle.release(true)
              }
            },
          )
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

  override fun language(): GuestLanguage = GraalVMGuest.PYTHON

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings["Flask".asPublicJsSymbol()] = this
    bindings["request".asPublicJsSymbol()] = requestAccessor
  }

  override fun symbolicName(): String = "Flask"

  @Deprecated("Use symbolicName instead")
  override fun displayName(): String = "Flask"

  private companion object {
    private val logging by lazy { Logging.of(FlaskHttpIntrinsic::class) }

    private val INSTANCE_MEMBER_KEYS = arrayOf(
      "bind",
      "route",
      "get",
      "post",
      "put",
      "patch",
      "delete",
    )
  }
}
