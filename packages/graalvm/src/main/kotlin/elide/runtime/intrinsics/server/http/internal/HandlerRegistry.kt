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
package elide.runtime.intrinsics.server.http.internal

import kotlinx.atomicfu.atomic
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpRouter
import elide.vm.annotations.Polyglot

/**
 * A Handler Registry manages references to guest values that act as request handlers. [GuestHandler] references are
 * used for routing and are bound to a specific context.
 *
 * @see ThreadLocalHandlerRegistry
 */
@DelicateElideApi internal abstract class HandlerRegistry : HttpRouter {
  private val hasHandlers = atomic(false)

  /** Indicate whether handlers are bound. */
  open val isEmpty: Boolean get() = !hasHandlers.value

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

  // Internal access to register pure guest values. In this case, routing is delegated to the application.
  internal fun handle(guest: PolyglotValue) {
    require(guest.canExecute()) { "Guest server handlers must be executable" }
    register(GuestHandler.async(guest))
  }

  @Polyglot override fun handle(method: String?, path: String?, handler: PolyglotValue) {
    hasHandlers.value = true
    register(GuestHandler.simple(handler))
  }
}
