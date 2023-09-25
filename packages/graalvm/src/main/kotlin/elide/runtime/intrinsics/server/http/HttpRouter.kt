package elide.runtime.intriniscs.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intriniscs.server.http.internal.HandlerRegistry

/**
 * Base class providing route registration APIs to guest code, compiling routing keys that can be used to resolve
 * handler references from a [HandlerRegistry].
 */
@DelicateElideApi public interface HttpRouter {
  /** Guest-accessible method used to register a [handler] for the provided [method] and [path]. */
  @Export public fun handle(method: String?, path: String?, handler: PolyglotValue)

  public companion object {
    /**
     * Generate a stable key for a route handler, which combines the HTTP method and the path template.
     *
     * @param path The path template used by this handler, or `null` to match every path.
     * @param method The HTTP method handled by this route, or `null` to match every method.
     */
    internal fun compileRouteKey(path: String?, method: String?): String {
      return "${path ?: "*"}:${method ?: "*"}".lowercase()
    }
  }
}