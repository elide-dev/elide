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
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Event Target
 *
 * The [EventTarget] and [Event] objects are a Node.js-specific implementation of the
 * [EventTarget Web API](https://dom.spec.whatwg.org/#eventtarget) that are exposed by some Node.js core APIs.
 *
 * The [EventTarget] interface is implemented by objects that can receive events and may have listeners for them; events
 * are dispatched via [dispatchEvent], and listeners are managed via [addEventListener] and [removeEventListener].
 *
 * Event targets are implementable from host code or guest code.
 *
 * Event targets in Node.js-style dispatch form differ slightly from web-standard event targets; see below for more
 * information.
 *
 * ### Node.js `EventTarget` vs. DOM `EventTarget`
 *
 * There are two key differences between the Node.js [EventTarget] and the
 * [EventTarget Web API](https://dom.spec.whatwg.org/#eventtarget):
 *
 * - Whereas DOM [EventTarget] instances may be hierarchical, there is no concept of hierarchy and event propagation in
 *   Node.js. That is, an event dispatched to an [EventTarget] does not propagate through a hierarchy of nested target
 *   objects that may each have their own set of handlers for the event.
 *
 * - In the Node.js [EventTarget], if an event listener is an async function or returns a `Promise`, and the returned
 *   `Promise` rejects, the rejection is automatically captured and handled the same way as a listener that throws
 *   synchronously (see [EventTarget] error handling docs for details).
 */
@Implementable
@API public interface EventTarget : EventEmitterOrTarget {
  /**
   * Adds a new handler for the [type] event. Any given [listener] is added only once per [type] and per `capture`
   * option value.
   *
   * If the once option is `true`, the listener is removed after the next time a type event is dispatched.
   *
   * The `capture` option is not used by Node.js in any functional way other than tracking registered event listeners
   * per the [EventTarget] specification. Specifically, the `capture` option is used as part of the key when registering
   * a [listener]. Any individual [listener] may be added once with `capture = false`, and once with `capture = true`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun addEventListener(type: String, listener: EventListener)

  /**
   * Adds a new handler for the [type] event. Any given [listener] is added only once per [type] and per `capture`
   * option value.
   *
   * If the once option is `true`, the listener is removed after the next time a type event is dispatched.
   *
   * The `capture` option is not used by Node.js in any functional way other than tracking registered event listeners
   * per the [EventTarget] specification. Specifically, the `capture` option is used as part of the key when registering
   * a [listener]. Any individual [listener] may be added once with `capture = false`, and once with `capture = true`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   * @param options An option object that specifies characteristics about the event listener.
   */
  @Polyglot public fun addEventListener(type: String, listener: Value, options: Value?)

  /**
   * Adds a new handler for the [type] event. Any given [listener] is added only once per [type] and per `capture`
   * option value.
   *
   * If the once option is `true`, the listener is removed after the next time a type event is dispatched.
   *
   * The `capture` option is not used by Node.js in any functional way other than tracking registered event listeners
   * per the [EventTarget] specification. Specifically, the `capture` option is used as part of the key when registering
   * a [listener]. Any individual [listener] may be added once with `capture = false`, and once with `capture = true`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   * @param options An option object that specifies characteristics about the event listener.
   */
  @Polyglot public fun addEventListener(type: String, options: AddEventListenerOptions, listener: EventListener)

  /**
   * Dispatches the event to the list of handlers for [Event.type].
   *
   * The registered event listeners is synchronously invoked in the order they were registered.
   *
   * @param event The event to dispatch.
   * @return `true` if either event's cancelable attribute value is `false` or its `preventDefault()` method was not
   *   invoked, otherwise `false`.
   */
  @Polyglot public fun dispatchEvent(event: Event): Boolean

  /**
   * Removes the `listener` from the list of handlers for event `type`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun removeEventListener(type: String, listener: EventListener)

  /**
   * Removes the `listener` from the list of handlers for event `type`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   */
  @Polyglot public fun removeEventListener(type: String, listener: Value)

  /**
   * Removes the `listener` from the list of handlers for event `type`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   * @param options An option object that specifies characteristics about the event listener.
   */
  @Polyglot public fun removeEventListener(type: String, listener: Value, options: Value)

  /**
   * Removes the `listener` from the list of handlers for event `type`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   * @param options An option object that specifies characteristics about the event listener.
   */
  @Polyglot public fun removeEventListener(type: String, listener: EventListener, options: Value)

  /**
   * Removes the `listener` from the list of handlers for event `type`.
   *
   * @param type The name of the event.
   * @param listener The callback function.
   * @param options An option object that specifies characteristics about the event listener.
   */
  @Polyglot public fun removeEventListener(type: String, listener: EventListener, options: RemoveEventListenerOptions)
}
