package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * A Handler Registry manages references to guest values that act as request handlers. [GuestHandler] references are
 * used for routing and are bound to a specific context.
 *
 * @see ThreadLocalHandlerRegistry
 */
@DelicateElideApi internal interface HandlerRegistry {
  /**
   * Register a [handler] with the given [key].
   *
   * @param key A unique key to associate the handler with.
   * @param handler A reference to an executable guest value.
   */
  fun register(key: String, handler: GuestHandler)

  /**
   * Resolve a [GuestHandler] reference associated with the given [key].
   *
   * @param key The key used to retrieve the handler, previously used with [register].
   * @return A [GuestHandler] reference, or `null` if no handler with that [key] is found.
   */
  fun resolve(key: String): GuestHandler?
}