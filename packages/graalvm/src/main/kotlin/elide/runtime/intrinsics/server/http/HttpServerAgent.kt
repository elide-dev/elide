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
package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.Source
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.internals.GVMInvocationBindings
import elide.runtime.gvm.internals.intrinsics.installElideBuiltin
import elide.runtime.gvm.internals.js.JsExecutableScript
import elide.runtime.gvm.internals.js.JsInvocationBindings
import elide.runtime.intrinsics.server.http.internal.GuestHandler
import elide.runtime.intrinsics.server.http.internal.HandlerRegistry
import elide.runtime.intrinsics.server.http.internal.NoopServerEngine
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import elide.runtime.intrinsics.server.http.internal.ThreadLocalHandlerRegistry
import elide.runtime.intrinsics.server.http.netty.NettyServerConfig
import elide.runtime.intrinsics.server.http.netty.NettyServerEngine

/**
 * A Server Agent manages the lifecycle of HTTP Server intrinsics and their injection into guest code.
 *
 * Use the [run] method to start a new HTTP server that allows routing configuration to be specified by guest code from
 * a provided entry point.
 *
 * The server routing intrinsic is bound as `Elide.http.router`, regardless of the target language. For example, in
 * JavaScript, the following code may be used to retrieve it:
 * ```javascript
 * // the 'Elide' object is available as a top-level symbol
 * // no import statements are required to use it
 * const router = Elide.http.router;
 *
 * // handlers can be specified by calling the 'handle' method,
 * // this can be used to create wrappers simulating popular APIs
 * // in the target ecosystem (e.g. Express.js in JavaScript)
 * router.handle("GET", "/hello", (request, response, context) => {
 *   response.send(200, getMessage());
 * });
 * ```
 *
 * After evaluating the guest entry point, the HTTP server is automatically started, without requiring an explicit call
 * (such as `server.start()` or any similar constructs).
 */
@DelicateElideApi public class HttpServerAgent {
  private fun initializeForRegistry(
    handlerRegistry: HandlerRegistry,
    entrypoint: Source,
    value: PolyglotValue,
    router: PipelineRouter? = null,
  ): PolyglotValue {
    // if no handlers are registered by virtue of executing the entrypoint, it probably means the script handed back
    // a suite of exports, which we can use to resolve the entrypoint.
    return if (handlerRegistry.isEmpty) {
      JsInvocationBindings.entrypoint(
        script = JsExecutableScript.of(entrypoint),
        value = value,
      ).let { entry ->
        require(entry.bindings.supported().contains(GVMInvocationBindings.DispatchStyle.SERVER)) {
          "No server returned or configured by script"
        }
        // resolve the server entrypoint
        val boundEntry = when (val binding = entry.bindings) {
          is JsInvocationBindings.JsServer,
          is JsInvocationBindings.JsCompound -> requireNotNull(
            binding.mapped.values.find {
              it.info.type == JsInvocationBindings.JsEntrypointType.SERVER
            }
          )

          else -> error("Unsupported binding: Please configure or export a server from the entrypoint")
        }
        if (router == null) {
          handlerRegistry.register(GuestHandler.async(boundEntry.value))
        } else {
          router.handle(boundEntry.value)
        }
        check(!handlerRegistry.isEmpty) { "Failed to register guest handler" }
        boundEntry.value
      }
    } else {
      // fallback to the regular entrypoint
      value
    }
  }

  /**
   * Configure and start a new HTTP server, using the provided [entrypoint] to configure the routing and binding
   * behavior through injected intrinsics.
   *
   * The [acquireContext] function will be called multiple times to obtain thread-local [PolyglotContext] instances,
   * which is required to enable proper multithreaded request handling.
   *
   * @param entrypoint The guest code to be evaluated as server and routing configuration.
   * @param acquireContext Factory function used to obtain a [PolyglotContext] for the current thread.
   */
  public fun run(
    entrypoint: Source,
    execProvider: GuestExecutorProvider,
    acquireContext: () -> PolyglotContext
  ) {
    // a thread-local registry that re-evaluates the entrypoint on each thread to
    // obtain references to guest handler values
    val handlerRegistry = ThreadLocalHandlerRegistry { registry ->
      initializeHandlerRegistry(
        registry = registry,
        context = acquireContext(),
        entrypoint = entrypoint,
      )
    }

    // configure the router and server engine
    val config = NettyServerConfig()
    val router = PipelineRouter(handlerRegistry)
    val engine = NettyServerEngine(config, router, execProvider)

    // prepare a new context, injecting the router to allow guest code to register
    // handlers; this differs from thread-local initialization in that the router
    // also constructs a shared handler pipeline that does not require references
    acquireContext().apply {
      installEngine(engine)
      evaluate(entrypoint).also {
        initializeForRegistry(
          handlerRegistry = handlerRegistry,
          entrypoint = entrypoint,
          value = it,
          router = router,
        )

        // automatically start if requested
        if (config.autoStart || !handlerRegistry.isEmpty) engine.start()
      }
    }
  }

  /**
   * Initialize a thread-local [HandlerRegistry] by re-evaluating a given [entrypoint]. The [registry] is wrapped in
   * a [NoopServerEngine] and injected into the [context] before evaluating the guest configuration code.
   *
   * @param registry The thread-local registry being initialized.
   * @param context The context to be used for evaluating the [entrypoint], must belong to the current thread.
   * @param entrypoint Guest source containing configuration code to register request handlers.
   */
  private fun initializeHandlerRegistry(
    registry: HandlerRegistry,
    context: PolyglotContext,
    entrypoint: Source,
  ): PolyglotValue {
    // inject the registry into the context to allow guest code to register handlers,
    // this is required for correct initialization of the registry since thread-local
    // references to each handler are required
    installEngine(NoopServerEngine(NettyServerConfig(), registry))
    return context.evaluate(entrypoint).also {
      initializeForRegistry(
        handlerRegistry = registry,
        entrypoint = entrypoint,
        value = it,
      )
    }
  }

  public companion object {
    /** Binding path for the route builder. */
    internal const val ENGINE_BIND_PATH: String = "Elide.http"

    /** Install a server [engine] at the standard [ENGINE_BIND_PATH] in this context. */
    private fun installEngine(engine: HttpServerEngine) {
      installElideBuiltin("http", engine)
    }
  }
}
