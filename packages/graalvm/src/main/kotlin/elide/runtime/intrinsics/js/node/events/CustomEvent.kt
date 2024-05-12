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
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.annotations.API
import elide.vm.annotations.Polyglot

// All properties and methods for custom events which are exposed to guest code.
private val CUSTOM_EVENT_PROPS_AND_METHODS = arrayOf(
  "bubbles",
  "cancelable",
  "composed",
  "currentTarget",
  "defaultPrevented",
  "eventPhase",
  "isTrusted",
  "returnValue",
  "composedPath",
  "initEvent",
  "preventDefault",
  "stopImmediatePropagation",
  "stopPropagation",
  "srcElement",
  "target",
  "timeStamp",
  "type",
)

/**
 * ## Node API: Custom Event
 *
 * The [CustomEvent] object is an adaptation of the [CustomEvent Web API](https://dom.spec.whatwg.org/#customevent).
 * Instances are created internally by Node.js.
 */
@Implementable
@API public open class CustomEvent (
  typeName: String,
  detail: Any? = null,
) : Event, ProxyObject {
  // Empty constructor.
  @Polyglot public constructor(): this("")

  private val createdAt = System.currentTimeMillis().toDouble()
  private val initialized: AtomicBoolean = AtomicBoolean(typeName.isNotEmpty())
  private val eventType: AtomicReference<String> = AtomicReference(typeName)
  private val doesBubble: AtomicBoolean = AtomicBoolean(true)
  private val canCancel: AtomicBoolean = AtomicBoolean(true)
  private val isDispatching: AtomicBoolean = AtomicBoolean(false)
  private val isCancelled: AtomicBoolean = AtomicBoolean(false)
  private val stopDefault: AtomicBoolean = AtomicBoolean(false)
  private val stopPropagation: AtomicBoolean = AtomicBoolean(false)
  private val stopImmediate: AtomicBoolean = AtomicBoolean(false)
  private val isTrustedEvent: AtomicBoolean = AtomicBoolean(false)
  private val targetSuite: MutableList<EventTarget> = LinkedList()
  private val origin: AtomicReference<EventTarget> = AtomicReference()
  private val activeTarget: AtomicReference<EventTarget> = AtomicReference()
  private val detailData: AtomicReference<Any> = AtomicReference(detail)

  // Indicate whether propagation is enabled for this event; host-side only.
  internal val propagates: Boolean get() = doesBubble.get() && !stopPropagation.get() && !stopImmediate.get()

  @get:Polyglot override val bubbles: Boolean get() = doesBubble.get()
  @get:Polyglot override val cancelable: Boolean get() = canCancel.get()
  @get:Polyglot override val type: String get() = eventType.get()
  @get:Polyglot override val composed: Boolean get() = false
  @get:Polyglot override val eventPhase: Int get() = if (isDispatching.get()) 2 else 0

  /**
   * Return detail information about this event.
   */
  @get:Polyglot public open val detail: Any? get() = detailData.get()

  /**
   * Notify this event that it is under dispatch.
   *
   * @param target The target to dispatch to.
   */
  public fun notifyDispatch(target: EventTarget) {
    require(initialized.get()) { "Event has not been initialized" }
    require(!isDispatching.get()) { "Cannot dispatch an event while it is already dispatching." }
    isDispatching.compareAndSet(false, true)
    activeTarget.compareAndSet(null, target)
    origin.compareAndSet(null, target)
  }

  /**
   * Notify this event that it is considered "trusted".
   */
  public fun notifyTrusted() {
    isTrustedEvent.set(true)
  }

  /**
   * Notify this event that it is dispatching under a new target.
   */
  public fun notifyTarget(target: EventTarget) {
    targetSuite.add(activeTarget.get())
    activeTarget.set(target)
  }

  /**
   * Determine whether this event can continue to be dispatched.
   */
  public fun canDispatch(): Boolean {
    return !isCancelled.get() && !stopImmediate.get()
  }

  @get:Polyglot override val currentTarget: EventTarget? get() = activeTarget.get()
  @get:Polyglot override val defaultPrevented: Boolean get() = stopDefault.get()
  @get:Polyglot override val isTrusted: Boolean get() = isTrustedEvent.get()
  @get:Polyglot override val returnValue: Boolean get() = !defaultPrevented
  @get:Polyglot override val srcElement: EventTarget? get() = origin.get()
  @get:Polyglot override val target: EventTarget? get() = origin.get()
  @get:Polyglot override val timeStamp: Double get() = createdAt
  @Polyglot override fun composedPath(): Array<EventTarget> {
    val active = activeTarget.get()
    val suite = targetSuite

    if (active != null) {
      val path = ArrayList<EventTarget>(1 + suite.size)
      path.add(active)
      path.addAll(suite)
      return path.toTypedArray()
    }
    return emptyArray()
  }

  @Polyglot override fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean) {
    if (initialized.get()) error("Cannot re-initialize event")
    eventType.set(type)
    doesBubble.set(bubbles)
    canCancel.set(cancelable)
    initialized.compareAndSet(false, true)
  }

  @Polyglot override fun preventDefault() {
    stopDefault.set(true)
  }

  @Polyglot override fun stopImmediatePropagation() {
    stopImmediate.set(true)
  }

  @Polyglot override fun stopPropagation() {
    stopPropagation.set(true)
  }

  override fun getMemberKeys(): Array<String> = CUSTOM_EVENT_PROPS_AND_METHODS
  override fun hasMember(key: String): Boolean = key in CUSTOM_EVENT_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot set properties on a CustomEvent")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("Cannot remove properties from a CustomEvent")
  }

  override fun getMember(key: String?): Any? = when (key) {
    "bubbles" -> bubbles
    "cancelable" -> cancelable
    "composed" -> composed
    "currentTarget" -> currentTarget
    "defaultPrevented" -> defaultPrevented
    "eventPhase" -> eventPhase
    "isTrusted" -> isTrusted
    "returnValue" -> returnValue
    "srcElement" -> srcElement
    "target" -> target
    "timeStamp" -> timeStamp
    "type" -> type
    "preventDefault" -> ProxyExecutable { preventDefault() }
    "stopImmediatePropagation" -> ProxyExecutable { stopImmediatePropagation() }
    "stopPropagation" -> ProxyExecutable { stopPropagation() }
    "composedPath" -> ProxyExecutable { composedPath() }

    "initEvent" -> ProxyExecutable {
      val name = it.getOrNull(0)?.asString()
      val bubbles = it.getOrNull(1)?.asBoolean() ?: false
      val cancelable = it.getOrNull(2)?.asBoolean() ?: false
      require(name?.ifBlank { null } != null) { "Event type must be provided" }
      initEvent(name!!, bubbles, cancelable)
    }

    else -> null
  }
}
