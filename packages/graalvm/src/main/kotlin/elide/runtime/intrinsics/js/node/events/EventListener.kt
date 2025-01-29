/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Event Listener
 *
 * Event listeners registered for an event `type` may either be JavaScript functions or objects with a `handleEvent`
 * property whose value is a function.
 *
 * In either case, the handler function is invoked with the `event` argument passed to the `eventTarget.dispatchEvent()`
 * function.
 *
 * Async functions may be used as event listeners. If an async handler function rejects, the rejection is captured and
 * handled as described in [EventTarget error handling](https://nodejs.org/api/events.html#eventtarget-error-handling).
 *
 * An error thrown by one handler function does not prevent the other handlers from being invoked.
 *
 * The return value of a handler function is ignored.
 * Handlers are always invoked in the order they were added.
 * Handler functions may mutate the `event` object.
 *
 * ```javascript
 * function handler1(event) {
 *   console.log(event.type);  // Prints 'foo'
 *   event.a = 1;
 * }
 *
 * async function handler2(event) {
 *   console.log(event.type);  // Prints 'foo'
 *   console.log(event.a);  // Prints 1
 * }
 *
 * const handler3 = {
 *   handleEvent(event) {
 *     console.log(event.type);  // Prints 'foo'
 *   },
 * };
 *
 * const handler4 = {
 *   async handleEvent(event) {
 *     console.log(event.type);  // Prints 'foo'
 *   },
 * };
 *
 * const target = new EventTarget();
 *
 * target.addEventListener('foo', handler1);
 * target.addEventListener('foo', handler2);
 * target.addEventListener('foo', handler3);
 * target.addEventListener('foo', handler4, { once: true });
 * ```
 *
 * &nbsp;
 *
 * ### Host-side Listeners
 *
 * Coming soon.
 *
 * &nbsp;
 *
 * ### Guest-side Listeners
 *
 * Coming soon.
 *
 * &nbsp;
 *
 * @see Event for the event object passed to the handler function.
 * @see EventTarget for the event target that dispatches the event.
 * @see EventEmitter for the shape of types which can emit events.
 */
@Implementable
@FunctionalInterface
@API public fun interface EventListener {
  /**
   * Handle an event's occurrence and dispatch against a given [EventTarget].
   *
   * Events originate from at least one [EventTarget], and are typically spawned from within an [EventEmitter], although
   * that is not a requirement.
   *
   * @param event The event to handle.
   */
  @Polyglot public fun handleEvent(vararg event: Any?)
}
