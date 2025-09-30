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
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.DefaultLastHttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import jakarta.inject.Provider
import elide.annotations.Singleton
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

  public inner class FlaskAppInstance(private val root: String) : ProxyObject {
    private val stackBuilder = mutableListOf<Value>()

    override fun getMember(key: String?): Any? = when (key) {
      // generic routing decorator
      "route" -> ProxyExecutable { args ->
        // the outer decorator receives the arguments for the route
        val route = args.first().takeIf { it.isString }?.asString()
          ?: error("Expected a string argument for the routing path")

        // return inner decorator
        ProxyExecutable { innerArgs ->
          // the inner decorator receives the handler function itself and returns yet another function
          val handler = innerArgs.first()
          check(handler.canExecute()) { "Expected function for `route` handler" }

          stackBuilder.add(handler)
          ProxyExecutable { /* noop, calling the handler function is handled by the engine */ }
        }
      }

      "bind" -> ProxyExecutable {
        stackManager.withStack { it.addAll(stackBuilder) }
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
      val result = stack.asSequence()
        .map { it.execute(httpContext) }
        .last()

      // map return value here (e.g., use a String return value to create the response body)
      // ...

      // if the guest handler is async, schedule for execution and return a handle
      // otherwise finalize handling and send the response
      val content = Unpooled.copiedBuffer(result.asString(), Charsets.UTF_8)
      httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.writerIndex().toString())

      httpContext.responseBody.source(
        object : HttpContentSink.Producer {
          override fun pull(handle: HttpContentSink.Handle) {
            handle.push(DefaultLastHttpContent(content))
            handle.release(true)
          }
        },
      )

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
  }

  override fun symbolicName(): String = "Flask"

  @Deprecated("Use symbolicName instead")
  override fun displayName(): String = "Flask"

  private companion object {
    private val INSTANCE_MEMBER_KEYS = arrayOf(
      "bind",
      "route",
    )
  }
}
