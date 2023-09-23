package elide.runtime.intriniscs.server.http

import org.graalvm.polyglot.Source
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotContext
import elide.runtime.intriniscs.ElideBindings
import elide.runtime.intriniscs.server.http.internal.*
import elide.runtime.intriniscs.server.http.internal.HandlerRegistry
import elide.runtime.intriniscs.server.http.internal.HttpRouter
import elide.runtime.intriniscs.server.http.internal.HttpServerEngine
import elide.runtime.intriniscs.server.http.internal.RoutingRegistry

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
    val router = HttpRouter(handlerRegistry)
    val server = HttpServerEngine(router)

    // prepare a new context, injecting the router to allow guest code to register
    // handlers, this differs from thread-local initialization in that the router
    // also constructs a shared handler pipeline that does not require references
    val context = acquireContext()
    context.installRouter(router, entrypoint)
    context.evaluate(entrypoint)

    // TODO(@darvld): allow customizing this from guest code
    // start the server
    server.start(8080)
  }

  /**
   * Initialize a thread-local [HandlerRegistry] by re-evaluating a given [entrypoint]. The [registry] is wrapped in
   * a [RoutingRegistry] and injected into the [context] before evaluating the guest configuration code.
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
    context.installRouter(RoutingRegistry(registry), entrypoint)
    context.evaluate(entrypoint)
  }

  public companion object {
    /** Binding path for the route builder. */
    private const val ROUTER_BIND_PATH: String = "Elide.http.router"

    /** Install a [router] at the standard [ROUTER_BIND_PATH] in this context. */
    private fun PolyglotContext.installRouter(router: RoutingRegistry, entrypoint: Source) {
      ElideBindings.install(ROUTER_BIND_PATH, router, this, resolveLanguage(entrypoint))
    }

    // TODO(@darvld): use a cleaner approach here
    /** Hack used to resolve a [GuestLanguage] from a raw source without accessing language plugins. */
    private fun resolveLanguage(source: Source): GuestLanguage = object : GuestLanguage {
      override val languageId: String = source.language
    }
  }
}