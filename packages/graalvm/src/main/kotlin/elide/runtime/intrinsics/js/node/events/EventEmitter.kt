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
package elide.runtime.intrinsics.js.node.events

import org.graalvm.polyglot.HostAccess.Implementable
import org.graalvm.polyglot.Value
import java.io.Closeable
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Event Emitter
 *
 * The [EventEmitter] class is defined and exposed by the node:events module:
 *
 * ```js
 * const EventEmitter = require('node:events');
 * ```
 *
 * All EventEmitters emit the event `'newListener'` when new listeners are added and `'removeListener'` when existing
 * listeners are removed.
 *
 * It supports the following option:
 * - `captureRejections` <boolean> It enables automatic capturing of promise rejection. Default: false.
 */
@Implementable
@API public interface EventEmitter : EventEmitterOrTarget, Closeable, AutoCloseable {
  /**
   * Alias for `emitter.on(eventName, listener)`.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun addListener(eventName: String, listener: Value)

  /**
   * Alias for `emitter.on(eventName, listener)`.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun addListener(eventName: String, listener: EventListener)

  /**
   * Adds the listener function to the beginning of the listeners array for the event named eventName. No checks are
   * made to see if the listener has already been added.
   *
   * Multiple calls passing the same combination of eventName and listener will result in the listener being added, and
   * called, multiple times.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun prependListener(eventName: String, listener: Value)

  /**
   * Adds the listener function to the beginning of the listeners array for the event named eventName. No checks are
   * made to see if the listener has already been added.
   *
   * Multiple calls passing the same combination of eventName and listener will result in the listener being added, and
   * called, multiple times.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun prependListener(eventName: String, listener: EventListener)

  /**
   * Adds a one-time listener function for the event named eventName to the beginning of the listener array. The next
   * time eventName is triggered, this listener is removed, and then invoked.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun prependOnceListener(eventName: String, listener: Value)

  /**
   * Adds a one-time listener function for the event named eventName to the beginning of the listener array. The next
   * time eventName is triggered, this listener is removed, and then invoked.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun prependOnceListener(eventName: String, listener: EventListener)

  /**
   * Synchronously calls each of the listeners registered for the event named [eventName], in the order they were
   * registered, passing the supplied arguments to each.
   *
   * Returns true if the event had listeners, false otherwise.
   *
   * @param eventName The name of the event.
   * @param args The arguments to pass to the listeners.
   * @return `true` if the event had listeners, `false` otherwise.
   */
  @Polyglot public fun emit(eventName: String, vararg args: Any?): Boolean

  /**
   * Returns an array listing the events for which the emitter has registered listeners.
   * The values in the array are strings or `Symbol`s.
   *
   * @return An array listing the events for which the emitter has registered listeners.
   */
  @Polyglot public fun eventNames(): List<String>

  /**
   * Returns the current max listener value for the emitter.
   *
   * @return The current max listener value for the emitter.
   */
  @Polyglot public fun getMaxListeners(): Int

  /**
   * Sets the maximum number of listeners for the emitter.
   *
   * @param count The maximum number of listeners.
   */
  @Polyglot public fun setMaxListeners(count: Int)

  /**
   * Returns the number of listeners listening to the event named eventName.
   *
   * @param eventName The name of the event.
   * @return The number of listeners listening to the event named eventName.
   */
  @Polyglot public fun listenerCount(eventName: String): Int

  /**
   * Returns an array listing the listeners for the specified event.
   *
   * @param eventName The name of the event.
   * @return An array listing the listeners for the specified event.
   */
  @Polyglot public fun listeners(eventName: String): List<EventListener>

  /**
   * Alias for `emitter.removeListener(eventName, listener)`.
   *
   * @param eventName The name of the event.
   */
  @Polyglot public fun off(eventName: String, listener: Value)

  /**
   * Adds the [listener] function to the end of the listeners array for the event named [eventName]. No checks are made
   * to see if the listener has already been added.
   *
   * Multiple calls passing the same combination of [eventName] and [listener] will result in the listener being added,
   * and called, multiple times.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun on(eventName: String, listener: Value): EventEmitter

  /**
   * Adds the [listener] function to the end of the listeners array for the event named [eventName]. No checks are made
   * to see if the listener has already been added.
   *
   * Multiple calls passing the same combination of [eventName] and [listener] will result in the listener being added,
   * and called, multiple times.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun on(eventName: String, listener: EventListener): EventEmitter

  /**
   * Adds a one-time [listener] function for the event named [eventName]. The next time [eventName] is triggered, this
   * listener is removed and then invoked.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun once(eventName: String, listener: Value)
  /**

  * Adds a one-time [listener] function for the event named [eventName]. The next time [eventName] is triggered, this
   * listener is removed and then invoked.
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun once(eventName: String, listener: EventListener)

  /**
   * Removes all listeners, or those of the specified `eventName`.
   */
  @Polyglot public fun removeAllListeners()

  /**
   * Removes all listeners, or those of the specified [eventName].
   *
   * @param eventName The name of the event.
   */
  @Polyglot public fun removeAllListeners(eventName: String)

  /**
   * Removes the specified [listener] from the listener array for the event named [eventName].
   *
   * @param eventName The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun removeListener(eventName: String, listener: Value)

  /**
   * Returns a copy of the array of listeners for the event named [eventName], including any wrappers (such as those
   * created by `.once()`).
   *
   * @param eventName The name of the event.
   * @return A copy of the array of listeners for the event named [eventName].
   */
  @Polyglot public fun rawListeners(eventName: String): List<EventListener>
}
