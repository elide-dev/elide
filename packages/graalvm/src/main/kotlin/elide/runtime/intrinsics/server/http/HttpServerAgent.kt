package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.Source
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotContext
import elide.runtime.intrinsics.ElideBindings
import elide.runtime.intrinsics.server.http.internal.*
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
  /**
   * Configure and start a new HTTP server, using the provided [entrypoint] to configure the routing and binding
   * behavior through injected intrinsics.
   *
   * The [acquireContext] function will be called multiple times to obtain thread-local [PolyglotContext] instances,
   * which is required to enable proper multi-threaded request handling.
   *
   * @param entrypoint The guest code to be evaluated as server and routing configuration.
   * @param acquireContext Factory function used to obtain a [PolyglotContext] for the current thread.
   */
  public fun run(
    entrypoint: Source,
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
    val engine = NettyServerEngine(config, router)

    // prepare a new context, injecting the router to allow guest code to register
    // handlers, this differs from thread-local initialization in that the router
    // also constructs a shared handler pipeline that does not require references
    val context = acquireContext()
    context.installEngine(engine, entrypoint)
    context.evaluate(entrypoint)

    // automatically start if requested
    if (config.autoStart) engine.start()
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
  ) {
    // inject the registry into the context to allow guest code to register handlers,
    // this is required for correct initialization of the registry since thread-local
    // references to each handler are required
    context.installEngine(NoopServerEngine(NettyServerConfig(), registry), entrypoint)
    context.evaluate(entrypoint)
  }

  public companion object {
    /** Binding path for the route builder. */
    private const val ENGINE_BIND_PATH: String = "Elide.http"

    /** Install a server [engine] at the standard [ENGINE_BIND_PATH] in this context. */
    private fun PolyglotContext.installEngine(engine: HttpServerEngine, entrypoint: Source) {
      ElideBindings.install(ENGINE_BIND_PATH, engine, this, resolveLanguage(entrypoint))
    }

    // TODO(@darvld): use a cleaner approach here
    /** Hack used to resolve a [GuestLanguage] from a raw source without accessing language plugins. */
    private fun resolveLanguage(source: Source): GuestLanguage = object : GuestLanguage {
      override val languageId: String = source.language
    }
  }
}
