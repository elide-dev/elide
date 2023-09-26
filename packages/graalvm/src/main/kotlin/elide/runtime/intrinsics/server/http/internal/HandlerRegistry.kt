package elide.runtime.intrinsics.server.http.internal

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpRouter

/**
 * A Handler Registry manages references to guest values that act as request handlers. [GuestHandler] references are
 * used for routing and are bound to a specific context.
 *
 * @see ThreadLocalHandlerRegistry
 */
@DelicateElideApi internal abstract class HandlerRegistry : HttpRouter {
  /**
   * Register a [handler] with the given [key].
   *
   * @param key A unique key to associate the handler with.
   * @param handler A reference to an executable guest value.
   */
  abstract fun register(key: String, handler: GuestHandler)

  /**
   * Resolve a [GuestHandler] reference associated with the given [key].
   *
   * @param key The key used to retrieve the handler, previously used with [register].
   * @return A [GuestHandler] reference, or `null` if no handler with that [key] is found.
   */
  abstract fun resolve(key: String): GuestHandler?

  @Export override fun handle(method: String?, path: String?, handler: PolyglotValue) {
    register(HttpRouter.compileRouteKey(path, method), GuestHandler.of(handler))
  }
}