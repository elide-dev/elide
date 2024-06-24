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
@file:Suppress("DataClassPrivateConstructor")

package elide.runtime.gvm.internals.node.events

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlinx.collections.immutable.toImmutableList
import elide.annotations.API
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.Disposable
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.AbortSignal
import elide.runtime.intrinsics.js.node.EventsAPI
import elide.runtime.intrinsics.js.node.events.*
import elide.runtime.intrinsics.js.node.events.EventListener
import elide.vm.annotations.Polyglot

// Internal symbol where the Node built-in module is installed.
private const val EVENTS_MODULE_SYMBOL = "node_events"

// Public symbol where `EventTarget` is installed.
private const val EVENT_TARGET_SYMBOL = "EventTarget"

// Maximum number of listeners to set as a default value.
private const val DEFAULT_DEFAULT_MAX_LISTENERS = 10

// Installs the Node `events` built-in module.
@Intrinsic @Factory internal class NodeEventsModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): EventsAPI = NodeEventsModuleFacade.obtain()

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[EVENTS_MODULE_SYMBOL.asJsSymbol()] = NodeEventsModuleFacade.obtain()
    bindings[EVENT_TARGET_SYMBOL.asPublicJsSymbol()] = EventTarget::class.java
  }
}

// Convert a guest value into an `Event`.
private fun guestValueToEvent(value: Value): Event = when {
  // host-implemented events
  value.isHostObject -> value.asHostObject()

  // guest-implemented events
  value.metaObject.isHostObject && value.metaObject.metaQualifiedName == Event::class.qualifiedName ->
    value.asHostObject()

  else -> error("Unsupported event value")
}

// Convert a guest value into an `EventListener`.
private fun guestValueToListener(value: Value): EventListener = when {
  // host-implemented listeners
  value.isHostObject -> value.asHostObject()

  // guest-implemented listeners
  value.metaObject.isHostObject && value.metaObject.metaQualifiedName == EventListener::class.qualifiedName ->
    value.asHostObject()

  else -> error("Unsupported listener value")
}

/**
 * ## Node API: Standard Event Names
 *
 * Standard events which are dispatched in generic circumstances, or which are used internally by the Events API
 * implementation.
 */
internal object StandardEventName {
  /** Dispatched on `EventEmitter` instances when a new listener is added for any event. */
  const val NEW_LISTENER = "newListener"

  /** Dispatched on `EventEmitter` instances when an existing listener is removed for any event. */
  const val REMOVE_LISTENER = "removeListener"
}

/**
 * ## Node API: Host Event Listener
 *
 * Describes a host-implemented event listener, which functions as a [Consumer] of [Event] instances, within the context
 * of a given [EventListener].
 *
 * Host-side event listeners are additionally [ProxyExecutable], allowing dispatch from guest languages.
 *
 * @see GuestEventListener for a guest-side equivalent.
 */
@FunctionalInterface
@API public fun interface HostEventListener :
  Consumer<Any?>,
  EventListener,
  ProxyExecutable {
  // Delegate to the listener.
  override fun accept(event: Any?): Unit = handleEvent(event)

  @Polyglot override fun execute(vararg arguments: Value): Any? {
    handleEvent(guestValueToEvent(
      arguments.getOrNull(0) ?: throw JsError.typeError("Event argument is required")
    ))
    return null  // no return value
  }

  /** Factory utilities for host-side event listeners. */
  public companion object {
    /**
     * Generate a [HostEventListener] from a host-side lambda.
     *
     * @param listener The host-side event listener.
     * @return A new [HostEventListener] instance.
     */
    @JvmStatic public fun of(listener: (Any?) -> Unit): EventListener = HostEventListener { event ->
      listener.invoke(event)
    }
  }
}

/**
 * ## Node API: Guest Event Listener
 *
 * Describes a guest-implemented event listener, which functions as a [Consumer] of [Event] instances, within the
 * context of a given [EventListener], backed by a guest function or value.
 *
 * @see HostEventListener for a host-side equivalent.
 */
@FunctionalInterface
@API public fun interface GuestEventListener :
  Consumer<Event>,
  EventListener,
  ProxyExecutable {
  override fun accept(event: Event): Unit = handleEvent(event)

  @Polyglot override fun execute(vararg arguments: Value?): Any? {
    handleEvent(guestValueToEvent(
      arguments.getOrNull(0) ?: throw JsError.typeError("Event argument is required")
    ))
    return null  // no return value
  }

  /** Factory utilities for guest-side event listeners. */
  public companion object {
    /**
     * Generate a [GuestEventListener] from a guest-side function or other executable value.
     *
     * @param listener The guest-side event listener.
     * @return A new [GuestEventListener] instance.
     */
    @JvmStatic public fun of(listener: Value): EventListener = GuestEventListener { event ->
      listener.executeVoid(event)
    }
  }
}

// Represents an event listener which is bound to an event-aware target.
private data class BoundEventListener private constructor(
  val subject: WeakReference<EventTarget>,
  val listener: EventListener,
  val options: AddEventListenerOptions? = null,
) {
  companion object {
    // Bind an event listener to a target, with options.
    @JvmStatic fun bind(
      target: EventTarget,
      listener: EventListener,
      options: AddEventListenerOptions?,
    ): BoundEventListener = BoundEventListener(WeakReference(target), listener, options)
  }

  // Dispatch an event, accounting for self-removing listeners.
  fun dispatch(type: String, args: Array<out Any?>) {
    try {
      listener.handleEvent(*args)
    } finally {
      if (options?.once == true) {
        subject.get()?.removeEventListener(type, listener)
      }
    }
  }
}

// Alias to bind an event listener.
private fun EventListener.bind(target: EventTarget, options: AddEventListenerOptions? = null): BoundEventListener {
  return BoundEventListener.bind(target, this, options)
}

/**
 * # Event-Aware
 *
 * Describes a host-side object which behaves as an [EventTarget], with all the internal machinery necessary to receive,
 * dispatch, and manage events and event listeners.
 *
 * This interface and the accompanying default implementation are designed to be used as JVM proxies.
 * For example:
 *
 * ```kotlin
 * public class MyObject constructor (private val proxy: EventAware = EventAware.create()) : EventAware by proxy {
 *   // your object here
 * }
 * ```
 *
 * See the _Usage_ section for more information.
 *
 * &nbsp;
 *
 * ## Usage
 *
 * The `EventAware` interface is designed to be used as a JVM proxy for host-side objects which need to behave as
 * event-aware types.
 *
 * Listeners are managed internally on behalf of the proxied object, and can be added and removed using the methods
 * provided by the [EventTarget] interface.
 *
 * Once implemented, events can be dispatched to a custom implementation, or to the proxied implementation.
 * Objects are encouraged to dispatch their own events, originating internally, via [dispatchEvent].
 *
 * ## Interaction with Node Events
 *
 * Node events can be dispatched through this interface on arbitrary objects; note as well that guest-side objects can
 * implement [EventTarget].
 */
public interface EventAware : EventTarget, EventEmitter {
  /** Factory methods for event-aware implementations. */
  public companion object {
    /** @return Fresh instance of an internal event relay, which can be used for proxying. */
    @JvmStatic public fun create(): EventAware = EventAwareRelay()

    /** @return Fresh instance of an internal event relay, which can be used for proxying, with known events. */
    @JvmStatic public fun create(events: Iterable<String>): EventAware = EventAwareRelay(events.toSortedSet())

    /** @return Fresh instance of an internal event relay, which can be used for proxying, with known events. */
    @JvmStatic public fun create(vararg events: String): EventAware = EventAwareRelay(events.toSortedSet())
  }
}

// Implementation of an event-aware relay, which hosts event services for an encapsulating object.
private class EventAwareRelay (
  private val knownEvents: SortedSet<String>? = null,
) : EventAware {
  // Whether this event dispatch is still open for events.
  private val open: AtomicBoolean = AtomicBoolean(true)

  // Kept count of listeners across both categories.
  private val listenerCount: AtomicInteger = AtomicInteger(0)

  // Map of active bubble-phase listeners.
  private val bubbleListeners: MutableMap<String, LinkedList<BoundEventListener>> = ConcurrentSkipListMap()

  // Map of active capture-phase listeners.
  private val captureListeners: MutableMap<String, LinkedList<BoundEventListener>> = ConcurrentSkipListMap()

  // Maximum count of listeners allowed on this target.
  private var maxListenerLimit: Int? = null

  @Polyglot override fun addEventListener(type: String, listener: EventListener): Unit = addEventListener(
    type,
    AddEventListenerOptions.DEFAULTS,
    listener,
  )

  @Polyglot override fun addEventListener(type: String, listener: Value, options: Value?): Unit = addEventListener(
    type,
    options?.let { AddEventListenerOptions.fromGuest(options) } ?: AddEventListenerOptions.DEFAULTS,
    GuestEventListener.of(listener),
  )

  @Suppress("SameParameterValue")
  private fun prependEventListener(type: String, capture: Boolean, listener: BoundEventListener) {
    when (capture) {
      true -> captureListeners
      false -> bubbleListeners
    }.computeIfAbsent(type) {
      LinkedList()
    }.add(0, listener).also {
      listenerCount.incrementAndGet()
      emit(StandardEventName.NEW_LISTENER, type, listener)
    }
  }

  private fun appendEventListener(type: String, capture: Boolean, listener: BoundEventListener) {
    when (capture) {
      true -> captureListeners
      false -> bubbleListeners
    }.computeIfAbsent(type) {
      LinkedList()
    }.add(
      listener
    ).also {
      listenerCount.incrementAndGet()
      emit(StandardEventName.NEW_LISTENER, type, listener)
    }
  }

  override fun addEventListener(type: String, options: AddEventListenerOptions, listener: EventListener) {
    if (type.isEmpty() || type.isBlank()) throw JsError.valueError("Event type must be a non-empty string")
    appendEventListener(type, options.capture, listener.bind(this, options))
  }

  private fun dispatchProtect(block: () -> Unit) {
    try {
      block()
    } catch (e: Throwable) {
      // default: do nothing
    }
  }

  @Polyglot override fun dispatchEvent(event: Event): Boolean {
    val type = event.type
    if (type.isEmpty() || type.isBlank()) throw JsError.valueError("Event type must be a non-empty string")
    var didExecute = false

    sequence {
      // capture phase
      captureListeners[type]?.let { listeners ->
        yieldAll(listeners.asSequence().map { true to it })
      }

      // bubble phase
      bubbleListeners[type]?.let { listeners ->
        yieldAll(listeners.asSequence().map { true to it })
      }
    }.forEach { (_, listener) ->
      // relay exceptions to events and handler
      dispatchProtect {
        didExecute = true
        listener.dispatch(type, arrayOf(event))
      }
    }
    return didExecute
  }

  @Polyglot override fun removeEventListener(type: String, listener: EventListener) {
    removeEventListener(
      type,
      listener,
      RemoveEventListenerOptions.DEFAULTS,
    )
  }

  @Polyglot override fun removeEventListener(type: String, listener: Value) {
    removeEventListener(
      type,
      guestValueToListener(listener),
      RemoveEventListenerOptions.DEFAULTS,
    )
  }

  @Polyglot override fun removeEventListener(type: String, listener: Value, options: Value) {
    removeEventListener(
      type,
      guestValueToListener(listener),
      RemoveEventListenerOptions.fromGuest(options),
    )
  }

  @Polyglot override fun removeEventListener(type: String, listener: EventListener, options: Value) {
    removeEventListener(
      type,
      listener,
      RemoveEventListenerOptions.fromGuest(options),
    )
  }

  override fun removeEventListener(type: String, listener: EventListener, options: RemoveEventListenerOptions) {
    options.capture.let {
      when (it) {
        true -> captureListeners
        false -> bubbleListeners
      }
    }[type]?.removeIf { it.listener === listener }.also {
      if (it == true) {
        listenerCount.decrementAndGet()
        emit(StandardEventName.REMOVE_LISTENER, type, listener)
      }
    }
  }

  @Polyglot override fun addListener(eventName: String, listener: Value) =
    addEventListener(eventName, guestValueToListener(listener))

  override fun addListener(eventName: String, listener: EventListener) =
    addEventListener(eventName, listener)

  @Polyglot override fun prependListener(eventName: String, listener: Value) {
    prependListener(eventName, guestValueToListener(listener))
  }

  override fun prependListener(eventName: String, listener: EventListener) {
    prependEventListener(eventName, false, listener.bind(this))
  }

  @Polyglot override fun prependOnceListener(eventName: String, listener: Value) {
    prependOnceListener(
      eventName,
      guestValueToListener(listener),
    )
  }

  override fun prependOnceListener(eventName: String, listener: EventListener) {
    prependEventListener(
      eventName,
      false,
      listener.bind(this, AddEventListenerOptions(once = true)),
    )
  }

  @Polyglot override fun emit(eventName: String, vararg args: Any): Boolean {
    if (eventName.isEmpty() || eventName.isBlank())
      throw JsError.valueError("Event type must be a non-empty string")
    var didExecute = false

    sequence {
      // capture phase
      captureListeners[eventName]?.let { listeners ->
        yieldAll(listeners.asSequence().map { true to it })
      }

      // bubble phase
      bubbleListeners[eventName]?.let { listeners ->
        yieldAll(listeners.asSequence().map { true to it })
      }
    }.forEach { (_, listener) ->
      // relay exceptions to events and handler
      dispatchProtect {
        didExecute = true
        listener.dispatch(eventName, args)
      }
    }
    return didExecute
  }

  @Polyglot override fun eventNames(): List<String> = knownEvents?.toImmutableList() ?: emptyList()
  @Polyglot override fun getMaxListeners(): Int = maxListenerLimit ?: DEFAULT_DEFAULT_MAX_LISTENERS
  @Polyglot override fun setMaxListeners(count: Int) { maxListenerLimit = count }
  @Polyglot override fun listenerCount(eventName: String): Int = if (eventName.isEmpty()) {
    listenerCount.get()
  } else {
    (captureListeners[eventName]?.size ?: 0) + (bubbleListeners[eventName]?.size ?: 0)
  }

  @Polyglot override fun listeners(eventName: String): List<EventListener> = sequence {
    require(eventName.isNotEmpty()) { "Cannot query listeners for empty event name" }

    // capture phase
    captureListeners[eventName]?.asSequence()?.map { true to it }?.let { yieldAll(it) }

    // bubble phase
    bubbleListeners[eventName]?.asSequence()?.map { false to it }?.let { yieldAll(it) }
  }.map {
    it.second.listener
  }.toImmutableList()

  @Polyglot override fun off(eventName: String, listener: Value): Unit = removeEventListener(eventName, listener)

  @Polyglot override fun on(eventName: String, listener: Value): EventEmitter = apply {
    addEventListener(eventName, guestValueToListener(listener))
  }

  override fun on(eventName: String, listener: EventListener) = apply {
    addEventListener(eventName, listener)
  }

  @Polyglot override fun once(eventName: String, listener: Value) =
    addEventListener(eventName, AddEventListenerOptions(once = true), guestValueToListener(listener))

  override fun once(eventName: String, listener: EventListener) =
    addEventListener(eventName, AddEventListenerOptions(once = true), listener)

  @Polyglot override fun removeAllListeners() {
    listenerCount.set(0)
    bubbleListeners.clear()
    captureListeners.clear()
  }

  @Polyglot override fun removeAllListeners(eventName: String) {
    val bubble = bubbleListeners[eventName]
    val capture = captureListeners[eventName]
    val total = (bubble?.size ?: 0) + (capture?.size ?: 0)
    listenerCount.addAndGet(-total)
    bubbleListeners.remove(eventName)
    captureListeners.remove(eventName)
  }

  @Polyglot override fun removeListener(eventName: String, listener: Value): Unit =
    removeEventListener(eventName, listener)

  override fun rawListeners(eventName: String): List<EventListener> = listeners(eventName)

  override fun close() {
    // clear all listeners forcibly
    open.compareAndSet(false, true)
    removeAllListeners()
  }
}

// Module facade which satisfies the built-in `events` module.
@Singleton internal class NodeEventsModuleFacade private constructor () : EventsAPI {
  private val currentErrorMonitor: AtomicReference<Any> = AtomicReference(null)
  private val doCaptureRejections: AtomicBoolean = AtomicBoolean(false)
  private val targetMaxListenerOverrides: MutableMap<EventTarget, Int> = mutableMapOf()
  private var currentMaxListeners: Int? = null

  @get:Polyglot @set:Polyglot override var defaultMaxListeners: Int
    get() = currentMaxListeners ?: DEFAULT_DEFAULT_MAX_LISTENERS
    set(value) { currentMaxListeners = value }

  @get:Polyglot @set:Polyglot override var errorMonitor: Any?
    get() = currentErrorMonitor.get()
    set(value) { currentErrorMonitor.set(value) }

  @get:Polyglot override val captureRejections: Boolean
    get() = doCaptureRejections.get()

  @Polyglot override fun getEventListeners(emitterOrTarget: EmitterOrTarget, event: String): List<EventListener> {
    return when (emitterOrTarget) {
      is EventEmitter -> emitterOrTarget.listeners(event)
      is EventTarget -> TODO("`getEventListeners` is not implemented yet for `EventTarget`")
      else -> throw JsError.typeError("Invalid emitter or target")
    }
  }

  @Polyglot override fun getMaxListeners(emitterOrTarget: EmitterOrTarget): Int = when (emitterOrTarget) {
    is EventEmitter -> emitterOrTarget.getMaxListeners()
    is EventTarget -> targetMaxListenerOverrides[emitterOrTarget] ?: defaultMaxListeners
    else -> throw JsError.typeError("Invalid emitter or target")
  }

  @Polyglot override fun setMaxListeners(count: Int, vararg emittersOrTargets: EmitterOrTarget) {
    if (emittersOrTargets.isEmpty()) {
      currentMaxListeners = count
    } else emittersOrTargets.forEach {
      when (it){
        is EventEmitter -> it.setMaxListeners(count)
        is EventTarget -> targetMaxListenerOverrides[it] = count
        else -> throw JsError.typeError("Invalid emitter or target")
      }
    }
  }

  @Polyglot override fun once(emitter: EventEmitter, name: String): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  @Polyglot override fun once(emitter: EventEmitter, name: String, options: Value): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  @Polyglot override fun once(emitter: EventEmitter, name: String, options: EventsOnceOptions): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  @Polyglot override fun listenerCount(emitter: Value, event: String): Int {
    TODO("Not yet implemented")
  }

  @Polyglot override fun listenerCount(emitter: EventEmitterOrTarget, eventName: String): Int = when (emitter) {
    is EventEmitter -> emitter.listenerCount(eventName)
    is EventTarget -> TODO("`listenerCount` is not implemented yet for `EventTarget`")
  }

  @Polyglot override fun on(emitter: EventEmitter, name: String) {
    TODO("Not yet implemented")
  }

  @Polyglot override fun on(emitter: EventEmitter, name: String, options: Value) {
    TODO("Not yet implemented")
  }

  @Polyglot override fun on(emitter: EventEmitter, name: String, options: EventsOnceOptions) {
    TODO("Not yet implemented")
  }

  @Polyglot override fun addAbortListener(signal: AbortSignal, listener: EventListener): Disposable {
    TODO("Not yet implemented")
  }

  @Polyglot override fun getMember(key: String): Any? = when (key) {
    "Event" -> Event::class.java
    "EventTarget" -> EventTarget::class.java
    "EventListener" -> EventListener::class.java
    "EventEmitter" -> EventEmitter::class.java
    "EventEmitterAsyncResource" -> TODO("Not yet implemented: `EventEmitterAsyncResource`")
    "defaultMaxListeners" -> defaultMaxListeners
    "errorMonitor" -> errorMonitor
    "captureRejections" -> captureRejections

    "getMaxListeners" -> ProxyExecutable {
      getMaxListeners(
        it.getOrNull(0) ?: throw JsError.valueError("`getMaxListeners` requires argument")
      )
    }

    "setMaxListeners" -> ProxyExecutable {
      val countValue = it.getOrNull(0)
        ?: throw JsError.valueError("`setMaxListeners` requires count argument")
      if (!countValue.isNumber || !countValue.fitsInShort())
        throw JsError.typeError("Max listener count must be a number")

      // decode count
      val count = countValue.asInt()
      if (count < 0)
        throw JsError.rangeError("Max listener count must be non-negative")

      val subject = it.getOrNull(1)
      ?: throw JsError.valueError("`setMaxListeners` requires target argument")

      setMaxListeners(
        count,
        subject,
      )
    }

    "getEventListeners" -> ProxyExecutable {
      val targetValue = it.getOrNull(0)
        ?: throw JsError.valueError("`getEventListeners` requires target argument")
      val eventValue = it.getOrNull(1)
        ?: throw JsError.valueError("`getEventListeners` requires event argument")
      if (!eventValue.isString)
        throw JsError.valueError("`getEventListeners` event name must be a string")

      getEventListeners(
        targetValue,
        eventValue.asString(),
      )
    }

    "listenerCount" -> ProxyExecutable {
      val targetValue = it.getOrNull(0)
        ?: throw JsError.valueError("`listenerCount` requires target argument")
      val eventValue = it.getOrNull(1)
        ?: throw JsError.valueError("`listenerCount` requires event argument")
      if (!eventValue.isString)
        throw JsError.valueError("`listenerCount` event name must be a string")

      listenerCount(
        targetValue,
        eventValue.asString(),
      )
    }

    "on",
    "once",
    "addAbortListener" -> null

    else -> null
  }

  companion object {
    private val SINGLETON: EventsAPI by lazy { create() }

    @JvmStatic fun create(): EventsAPI = NodeEventsModuleFacade()
    @JvmStatic fun obtain(): EventsAPI = SINGLETON
  }
}
