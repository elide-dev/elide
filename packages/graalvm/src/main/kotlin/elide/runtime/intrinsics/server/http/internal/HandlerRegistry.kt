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
   * Register a new [handler] reference, assigned to the next stage in the pipeline.
   *
   * @param handler A reference to an executable guest value.
   * @return The index of the stage to which the handler is associated.
   */
  abstract fun register(handler: GuestHandler): Int

  /**
   * Resolve a [GuestHandler] reference associated with the given [stage].
   *
   * @param stage The stage key (index) used to retrieve the handler, previously return by [register].
   * @return A [GuestHandler] reference, or `null` if no handler with that [stage] is found.
   */
  abstract fun resolve(stage: Int): GuestHandler?

  @Export override fun handle(method: String?, path: String?, handler: PolyglotValue) {
    register(GuestHandler.of(handler))
  }
}