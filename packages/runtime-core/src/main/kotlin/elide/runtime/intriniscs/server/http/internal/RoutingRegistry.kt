package elide.runtime.intriniscs.server.http.internal

import io.netty.handler.codec.http.HttpMethod
import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Base class providing route registration APIs to guest code, compiling routing keys that can be used to resolve
 * handler references from a [HandlerRegistry].
 */
@DelicateElideApi internal open class RoutingRegistry internal constructor(
  protected val handlerRegistry: HandlerRegistry
) {
  /** Private logger instance. */
  private val logging by lazy { Logging.of(RoutingRegistry::class) }

  /** Register a route handler with the given [method] and [path]. */
  protected open fun handle(method: HttpMethod?, path: String?, handler: PolyglotValue) {
    handlerRegistry.register(compileRouteKey(path, method), GuestHandler.of(handler))
  }

  /** Guest-accessible method used to register a[handler] for the provided [method] and [path]. */
  @Export fun handle(method: String?, path: String?, handler: PolyglotValue) {
    logging.trace { "Registering handler with method '$method' and path '$path'" }
    handle(method?.let { HttpMethod.valueOf(it) }, path, handler)
  }

  protected companion object {
    /**
     * Generate a stable key for a route handler, which combines the HTTP method and the path template.
     *
     * @param path The path template used by this handler, or `null` to match every path.
     * @param method The HTTP method handled by this route, or `null` to match every method.
     */
    fun compileRouteKey(path: String?, method: HttpMethod?): String {
      return "${path ?: "*"}:${method ?: "*"}".lowercase()
    }
  }
}