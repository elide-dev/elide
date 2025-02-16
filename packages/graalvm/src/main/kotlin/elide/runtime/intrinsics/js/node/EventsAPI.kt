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
@file:Suppress("StructuralWrap")

package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.runtime.intrinsics.js.AbortSignal
import elide.runtime.intrinsics.js.Disposable
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.events.*
import elide.vm.annotations.Polyglot

// Members from the Events API.
private val EVENTS_API_PROPS_AND_METHODS = arrayOf(
  "defaultMaxListeners",
  "errorMonitor",
  "captureRejections",
  "getEventListeners",
  "getMaxListeners",
  "setMaxListeners",
  "once",
  "listenerCount",
  "on",
  "addAbortListener",
)

/**
 * # Node API: Events
 *
 * Describes the surface of the `events` built-in module provided as part of the Node API; the `events` API provides
 * types like [EventEmitter], [EventTarget], [Event], and [EventListener] to work with events in Node.js.
 *
 * Events in Node behave similarly to DOM (browser) events, with a few caveats.
 *
 * For information about how the Node API behaves and how it differs from browsers, see the
 * [Node.js Events documentation](https://nodejs.org/dist/latest-v16.x/docs/api/events.html).
 *
 * ## Node Events
 *
 * Much of the Node.js core API is built around an idiomatic asynchronous event-driven architecture in which certain
 * kinds of objects (called "emitters") emit named events that cause Function objects ("listeners") to be called.
 *
 * For instance: a `net.Server` object emits an event each time a peer connects to it; a `fs.ReadStream` emits an event
 * when the file is opened; a stream emits an event whenever data is available to be read.
 *
 * All objects that emit events are instances of the [EventEmitter] class. These objects expose an [EventEmitter.on]
 * function that allows one or more functions to be attached to named events emitted by the object. Typically, event
 * names are camel-cased strings but any valid JavaScript property key can be used.
 *
 * When the [EventEmitter] object emits an event, all functions attached to that specific event are called
 * synchronously. Any values returned by the called listeners are ignored and discarded.
 *
 * The following example shows a simple [EventEmitter] instance with a single listener. The [EventEmitter.on] method
 * is used to register listeners, while the [EventEmitter.emit] method is used to trigger the event:
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 *
 * class MyEmitter extends EventEmitter {}
 *
 * const myEmitter = new MyEmitter();
 * myEmitter.on('event', () => {
 *   console.log('an event occurred!');
 * });
 * myEmitter.emit('event');
 * ```
 *
 * &nbsp;
 *
 * ### Passing arguments and `this` to listeners
 *
 * The [EventEmitter.emit] method allows an arbitrary set of arguments to be passed to the listener functions. Keep in
 * mind that when an ordinary listener function is called, the standard `this` keyword is intentionally set to reference
 * the [EventEmitter] instance to which the listener is attached.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * myEmitter.on('event', function(a, b) {
 *   console.log(a, b, this, this === myEmitter);
 *   // Prints:
 *   //   a b MyEmitter {
 *   //     _events: [Object: null prototype] { event: [Function (anonymous)] },
 *   //     _eventsCount: 1,
 *   //     _maxListeners: undefined,
 *   //     [Symbol(shapeMode)]: false,
 *   //     [Symbol(kCapture)]: false
 *   //   } true
 * });
 * myEmitter.emit('event', 'a', 'b');
 * ```
 *
 * It is possible to use ES6 Arrow Functions as listeners, however, when doing so, the `this` keyword will no longer
 * reference the EventEmitter instance:
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * myEmitter.on('event', (a, b) => {
 *   console.log(a, b, this);
 *   // Prints: a b {}
 * });
 * myEmitter.emit('event', 'a', 'b');
 * ```
 *
 * &nbsp;
 *
 * ### Asynchronous vs. synchronous
 *
 * The [EventEmitter] calls all listeners synchronously in the order in which they were registered. This ensures the
 * proper sequencing of events and helps avoid race conditions and logic errors. When appropriate, listener functions
 * can switch to an asynchronous mode of operation using the `setImmediate()` or `process.nextTick()` methods:
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * myEmitter.on('event', (a, b) => {
 *   setImmediate(() => {
 *     console.log('this happens asynchronously');
 *   });
 * });
 * myEmitter.emit('event', 'a', 'b');
 * ```
 *
 * &nbsp;
 *
 * ### Handling events only once
 *
 * When a listener is registered using the [EventEmitter.on] method, that listener is invoked every time the named event
 * is emitted.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * let m = 0;
 * myEmitter.on('event', () => {
 *   console.log(++m);
 * });
 * myEmitter.emit('event');
 * // Prints: 1
 * myEmitter.emit('event');
 * // Prints: 2
 * ```
 *
 * Using the [EventEmitter.once] method, it is possible to register a listener which is called at most once for a
 * particular event. Once the event is emitted, the listener is unregistered and then called.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * let m = 0;
 * myEmitter.once('event', () => {
 *   console.log(++m);
 * });
 * myEmitter.emit('event');
 * // Prints: 1
 * myEmitter.emit('event');
 * // Ignored
 * ```
 *
 * &nbsp;
 *
 * ### Error events
 *
 * When an error occurs within an [EventEmitter] instance, the typical action is for an `'error'` event to be emitted.
 * These are treated as special cases within Node.js.
 *
 * If an [EventEmitter] does not have at least one listener registered for the `'error'` event, and an `'error'` event
 * is emitted, the error is thrown, a stack trace is printed, and the Node.js process exits.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * myEmitter.emit('error', new Error('whoops!'));
 * // Throws and crashes Node.js
 * ```
 *
 * To guard against crashing the Node.js process the `domain` module can be used. (Note, however, that the `node:domain`
 * module is deprecated.)
 *
 * As a best practice, listeners should always be added for the `'error'` events.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * class MyEmitter extends EventEmitter {}
 * const myEmitter = new MyEmitter();
 * myEmitter.on('error', (err) => {
 *   console.error('whoops! there was an error');
 * });
 * myEmitter.emit('error', new Error('whoops!'));
 * // Prints: whoops! there was an error
 * ```
 *
 * It is possible to monitor `'error'` events without consuming the emitted error by installing a listener using the
 * symbol `events.errorMonitor`.
 *
 * ```javascript
 * import { EventEmitter, errorMonitor } from 'node:events';
 *
 * const myEmitter = new EventEmitter();
 * myEmitter.on(errorMonitor, (err) => {
 *   MyMonitoringTool.log(err);
 * });
 * myEmitter.emit('error', new Error('whoops!'));
 * // Still throws and crashes Node.js
 * ```
 *
 * &nbsp;
 *
 * ### Capture rejections of promises
 *
 * Using `async` functions with event handlers is problematic, because it can lead to an unhandled rejection in case of
 * a thrown exception:
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * const ee = new EventEmitter();
 * ee.on('something', async (value) => {
 *   throw new Error('kaboom');
 * });
 * ```
 *
 * The `captureRejections` option in the [EventEmitter] constructor or the global setting change this behavior,
 * installing a `.then(undefined, handler)` handler on the Promise. This handler routes the exception asynchronously to
 * the `Symbol.for('nodejs.rejection')` method if there is one, or to `'error'` event handler if there is none.
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 * const ee1 = new EventEmitter({ captureRejections: true });
 * ee1.on('something', async (value) => {
 *   throw new Error('kaboom');
 * });
 *
 * ee1.on('error', console.log);
 *
 * const ee2 = new EventEmitter({ captureRejections: true });
 * ee2.on('something', async (value) => {
 *   throw new Error('kaboom');
 * });
 *
 * ee2[Symbol.for('nodejs.rejection')] = console.log;
 * ```
 *
 * Setting `events.captureRejections = true` will change the default for all new instances of [EventEmitter].
 *
 * ```javascript
 * import { EventEmitter } from 'node:events';
 *
 * EventEmitter.captureRejections = true;
 * const ee1 = new EventEmitter();
 * ee1.on('something', async (value) => {
 *   throw new Error('kaboom');
 * });
 *
 * ee1.on('error', console.log);
 * ```
 *
 * The `'error'` events that are generated by the `captureRejections` behavior do not have a catch handler to avoid
 * infinite error loops: the recommendation is to not use async functions as `'error'` event handlers.
 *
 * @see Event for the base class for all events
 * @see CustomEvent for the extension point used to create custom event types
 * @see EventTarget for the interface supported for targets, or subjects, of events
 * @see EventEmitter for the interface supported for objects that emit events
 */
@API public interface EventsAPI : NodeAPI, ProxyObject {
  /**
   * By default, a maximum of 10 listeners can be registered for any single event. This limit can be changed for
   * individual EventEmitter instances using the `emitter.setMaxListeners(n) `method. To change the default for all
   * EventEmitter instances, the events.defaultMaxListeners property can be used. If this value is not a positive
   * number, a RangeError is thrown.
   *
   * Take caution when setting the events.defaultMaxListeners because the change affects all EventEmitter instances,
   * including those created before the change is made. However, calling `emitter.setMaxListeners(n)` still has
   * precedence over `events.defaultMaxListeners`.
   *
   * This is not a hard limit. The EventEmitter instance will allow more listeners to be added but will output a trace
   * warning to stderr indicating that a "possible EventEmitter memory leak" has been detected. For any single
   * EventEmitter, the emitter.getMaxListeners() and emitter.setMaxListeners() methods can be used to temporarily
   * avoid this warning:
   *
   * ```js
   * const EventEmitter = require('node:events');
   * const emitter = new EventEmitter();
   * emitter.setMaxListeners(emitter.getMaxListeners() + 1);
   * emitter.once('event', () => {
   *   // do stuff
   *   emitter.setMaxListeners(Math.max(emitter.getMaxListeners() - 1, 0));
   * });
   * ```
   *
   * The `--trace-warnings` command-line flag can be used to display the stack trace for such warnings.
   *
   * The emitted warning can be inspected with `process.on('warning')` and will have the additional `emitter`, `type`,
   * and `count` properties, referring to the event emitter instance, the event's name and the number of attached
   * listeners, respectively.
   *
   * Its name property is set to 'MaxListenersExceededWarning'.
   */
  @get:Polyglot @set:Polyglot public var defaultMaxListeners: Int

  /**
   * This symbol shall be used to install a listener for only monitoring `'error'` events. Listeners installed using
   * this symbol are called before the regular `'error'` listeners are called.
   *
   * Installing a listener using this symbol does not change the behavior once an `'error'` event is emitted. Therefore,
   * the process will still crash if no regular `'error'` listener is installed.
   */
  @get:Polyglot @set:Polyglot public var errorMonitor: Any?

  /**
   * If set to `true`, the `'rejectionHandled'` event is emitted whenever a Promise is rejected, but there are no
   * listeners to handle the rejection. This event is emitted with the following arguments:
   *
   * - `Promise` the Promise that was rejected.
   *
   * This event is useful for detecting and keeping track of promises that were rejected and not handled.
   */
  @get:Polyglot public val captureRejections: Boolean

  /**
   * Returns an array listing the events for which the emitter has registered listeners. The values in the array are
   * strings or Symbols.
   *
   * @param emitterOrTarget The EventEmitter or target object to query.
   * @param event The name of the event to query.
   * @return An array listing the events for which the emitter has registered listeners.
   */
  @Polyglot public fun getEventListeners(emitterOrTarget: EmitterOrTarget, event: String): List<EventListener>

  /**
   * Returns the current max listener value for the given emitter or target.
   *
   * @param emitterOrTarget The EventEmitter or target object to query.
   * @return The current max listener value for the given emitter or target.
   */
  @Polyglot public fun getMaxListeners(emitterOrTarget: EmitterOrTarget): Int

  /**
   * Sets the maximum number of listeners for the given emitter or target.
   *
   * @param count The maximum number of listeners.
   * @param emittersOrTargets The EventEmitter or target objects to set the maximum number of listeners for.
   */
  @Polyglot public fun setMaxListeners(count: Int, vararg emittersOrTargets: EmitterOrTarget)

  /**
   * Adds a one-time listener function for the event named `name` to the `emitter`. The next time `name` is triggered,
   * this listener is removed and then invoked.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   */
  @Polyglot public fun once(emitter: EventEmitter, name: String): JsPromise<Unit>

  /**
   * Adds a one-time listener function for the event named `name` to the `emitter`. The next time `name` is triggered,
   * this listener is removed and then invoked.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   * @param options The options to use when adding the listener.
   */
  @Polyglot public fun once(emitter: EventEmitter, name: String, options: Value): JsPromise<Unit>

  /**
   * Adds a one-time listener function for the event named `name` to the `emitter`. The next time `name` is triggered,
   * this listener is removed and then invoked.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   * @param options The options to use when adding the listener.
   */
  @Polyglot public fun once(emitter: EventEmitter, name: String, options: EventsOnceOptions): JsPromise<Unit>

  /**
   * Returns the number of listeners listening to the event named `event`.
   *
   * @param emitter The EventEmitter to query.
   * @param event The name of the event to query.
   * @return The number of listeners listening to the event named `event`.
   */
  @Polyglot public fun listenerCount(emitter: Value, event: String): Int

  /**
   * Returns the number of listeners listening to the event named [eventName].
   *
   * @param emitter The [EventEmitter] or [EventTarget] to query.
   * @param eventName The name of the event to query.
   * @return The number of listeners listening to the event named [eventName].
   */
  @Polyglot public fun listenerCount(emitter: EventEmitterOrTarget, eventName: String): Int

  /**
   * Adds the listener function to the end of the listeners array for the event named `name` to the `emitter`.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   */
  @Polyglot public fun on(emitter: EventEmitter, name: String)

  /**
   * Adds the listener function to the end of the listeners array for the event named `name` to the `emitter`.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   * @param options The options to use when adding the listener.
   */
  @Polyglot public fun on(emitter: EventEmitter, name: String, options: Value)

  /**
   * Adds the listener function to the end of the listeners array for the event named `name` to the `emitter`.
   *
   * @param emitter The EventEmitter to add the listener to.
   * @param name The name of the event to listen for.
   * @param options The options to use when adding the listener.
   */
  @Polyglot public fun on(emitter: EventEmitter, name: String, options: EventsOnceOptions)

  /**
   * Listens once to the `abort` event on the provided `signal`.
   *
   * Listening to the `abort` event on abort signals is unsafe and may lead to resource leaks since another third party
   * with the signal can call `e.stopImmediatePropagation()`. Unfortunately Node.js cannot change this since it would
   * violate the web standard.
   *
   * Additionally, the original API makes it easy to forget to remove listeners.
   *
   * This API allows safely using `AbortSignals` in Node.js APIs by solving these two issues by listening to the event
   * such that `stopImmediatePropagation` does not prevent the listener from running.
   *
   * Returns a disposable so that it may be unsubscribed from more easily.
   */
  @Polyglot public fun addAbortListener(signal: AbortSignal, listener: EventListener): Disposable

  override fun getMemberKeys(): Array<String> = EVENTS_API_PROPS_AND_METHODS
  override fun hasMember(key: String): Boolean = key in EVENTS_API_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot mutate member on Events API")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("Cannot mutate member on Events API")
  }
}
