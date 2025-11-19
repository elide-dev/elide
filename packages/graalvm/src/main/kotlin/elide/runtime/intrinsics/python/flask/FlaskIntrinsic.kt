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
@file:Suppress("MnInjectionPoints")

package elide.runtime.intrinsics.python.flask

import com.oracle.truffle.api.CompilerDirectives
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.inject.Provider
import kotlin.io.path.Path
import elide.annotations.Singleton
import elide.runtime.Logging
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeExecutor
import elide.runtime.core.RuntimeLatch
import elide.runtime.exec.ContextLocal
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.HttpServerEngine
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.assembleUri
import elide.runtime.http.server.python.flask.FlaskHttpException
import elide.runtime.http.server.python.flask.FlaskRouter
import elide.runtime.http.server.python.flask.FlaskServerApplication
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.plugins.python.flask.FlaskAPI
import elide.runtime.gvm.GuestLanguage as GVMGuestLanguage

@Singleton public class FlaskIntrinsic(
  runtimeLatch: Provider<RuntimeLatch>,
  entrypointProvider: Provider<EntrypointRegistry>,
  runtimeExecutor: Provider<RuntimeExecutor>,
  serverEngine: Provider<HttpServerEngine>,
) : ProxyExecutable, ProxyObject, GuestIntrinsic, FlaskAPI {
  private val runtimeLatch: RuntimeLatch by lazy { runtimeLatch.get() }
  private val serverEngine: HttpServerEngine by lazy { serverEngine.get() }

  private val entrypointRegistry by lazy { entrypointProvider.get() }
  private val applicationRoot by lazy { entrypointProvider.get().acquire()?.path?.let { Path(it) }?.parent }

  private val requestAccessor = FlaskRequestAccessor()
  private val handlerExecutor by lazy { runtimeExecutor.get().acquire() }
  private val localFlaskRouter = ContextLocal<FlaskRouter>()

  private val started = AtomicBoolean(false)

  public inner class FlaskAppInstance(internal val name: String) : ProxyObject {
    init {
      // automatically bind when the runtime is ready to wait for long tasks
      runtimeLatch.onAwait { bindServer() }
    }

    override fun getMember(key: String?): Any? = when (key) {
      ROUTE_METHOD -> ProxyExecutable { args /* (name, rule, methods, handler) */ ->
        // TODO(@darvld): remove once all guest dispatch is mediated by the executor
        if (!handlerExecutor.onDispatchThread) return@ProxyExecutable null

        val endpoint = args.getOrNull(0)?.takeIf { it.isString }
          ?: error("Invalid route; pass a string to @app.route")
        val pattern = args.getOrNull(1)?.takeIf { it.isString }
          ?: error("Expected route pattern; check guest wrapper")
        val methods = args.getOrNull(2)?.takeIf { it.hasArrayElements() }
          ?: error("Expected methods; check guest wrapper")
        val handler = requireNotNull(args.last()) { "no handler provided; check guest wrapper" }
        require(handler.canExecute()) { "guest python handler is not executable" }

        val router = localFlaskRouter.current() ?: FlaskRouter().also {
          handlerExecutor.setContextLocal(localFlaskRouter, it)
        }

        val mappedMethods = Array(methods.arraySize.toInt()) { method ->
          HttpMethod.valueOf(methods.getArrayElement(method.toLong()).asString())
        }

        router.register(endpoint.asString(), pattern.asString(), mappedMethods, handler)
      }

      BIND_METHOD -> ProxyExecutable { bindServer() }

      else -> null
    }

    override fun getMemberKeys(): Any = INSTANCE_MEMBER_KEYS
    override fun hasMember(key: String?): Boolean = key != null && key in INSTANCE_MEMBER_KEYS
    override fun putMember(key: String?, value: Value?): Unit = error("Modifying the flask app object is not allowed")
  }

  @CompilerDirectives.TruffleBoundary
  override fun execute(vararg arguments: Value?): FlaskAppInstance {
    // accept the application root as an argument
    val root = arguments.firstOrNull()?.takeIf { it.isString }?.asString()
      ?: error("The application package name must be specified")

    return FlaskAppInstance(root)
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
      // TODO(@darvld): remove once all guest dispatch is mediated by the executor
      if (!handlerExecutor.onDispatchThread) return@ProxyExecutable null

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

      localFlaskRouter.current()?.urlFor(endpoint, variables)
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

  private fun bindServer() {
    if (!started.compareAndSet(false, true)) return

    val serverApp = FlaskServerApplication(
      entrypoint = entrypointRegistry.acquire() ?: error("No entrypoint available"),
      applicationRoot = applicationRoot,
      executor = handlerExecutor,
      requestAccessor = requestAccessor,
      localRouter = localFlaskRouter,
    )

    val stack = serverEngine.bind(serverApp, HttpApplicationOptions())
    val service = stack.services.single { it.label == HttpCleartextService.LABEL }
      .bindResult.getOrThrow()

    logging.info { "Flask server listening at ${service.assembleUri()}" }
    runtimeLatch.retain()

    stack.onClose.whenComplete { shutdownErrors, err ->
      if (err != null) throw err
      if (shutdownErrors != null && shutdownErrors.isNotEmpty()) {
        logging.error("The server encountered errors when shutting down:")
        shutdownErrors.forEach { logging.error(it) }
      }
    }
  }

  private companion object {
    private val logging by lazy { Logging.of(FlaskIntrinsic::class) }

    private const val ROUTE_METHOD = "route"
    private const val BIND_METHOD = "bind"

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
