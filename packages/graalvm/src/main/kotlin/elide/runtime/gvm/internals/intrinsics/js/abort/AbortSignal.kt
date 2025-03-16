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
package elide.runtime.gvm.internals.intrinsics.js.abort

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import elide.runtime.exec.GuestExecutor
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.events.CustomEvent
import elide.runtime.intrinsics.js.node.events.Event
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.node.events.EventAware
import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.js.AbortSignal as AbortSignalAPI

private const val ABORTED_PROP = "aborted"
private const val REASON_PROP = "reason"
private const val ABORT_EVENT_NAME = "abort"
private const val TIMED_OUT_REASON = "Timed out"
private const val THROW_IF_ABORTED_METHOD = "throwIfAborted"
private val TIMED_OUT_EXC = JsError.of(TIMED_OUT_REASON)

/**
 * ## Abortable
 *
 * Host-side abort-enabled types implement this interface, which gains the [assignAborted] method.
 */
public interface Abortable : AbortSignalAPI {
  /**
   * Mark this abort-able object as aborted.
   */
  public fun assignAborted(reason: Any? = null)
}

/**
 * # Abort Signal
 *
 * Implements `AbortSignal` and `AbortSignal.Factory` for use in JavaScript.
 */
public class AbortSignal private constructor (
  initialState: Boolean = false,
  abortedReason: Any? = null,
  private val events: EventAware = EventAware.create(),
) : Abortable, EventTarget by events, ProxyObject {
  // Atomic state of this abort signal.
  private val didAbort: AtomicBoolean = atomic(initialState)

  // Atomic reason this signal was aborted.
  private val abortReason: AtomicRef<Any?> = atomic(abortedReason)

  private val gate: Boolean get() = didAbort.value
  private val reasonInfo: Any? get() = abortReason.value

  // Internal method to cause this abort signal to show as aborted; called from `AbortController` or runtime internals.
  override fun assignAborted(reason: Any?) {
    didAbort.value = true
    abortReason.value = reason
    events.emit(ABORT_EVENT_NAME, CustomEvent(ABORT_EVENT_NAME, reason))
  }

  @get:Polyglot override val aborted: Boolean get() = gate
  @get:Polyglot override val reason: Any? get() = reasonInfo

  @Polyglot override fun throwIfAborted() {
    if (aborted) when (val cause = reason) {
      is Throwable -> throw cause
      else -> when (cause) {
        is Value -> when {
          cause.isString -> throw JsError.of(cause.asString())
          else -> throw JsError.of(cause.toString())
        }
        else -> throw JsError.of(cause?.toString() ?: "Aborted")
      }
    }
  }

  override fun getMemberKeys(): Array<String> = arrayOf(
    ABORTED_PROP,
    REASON_PROP,
    THROW_IF_ABORTED_METHOD,
  )

  override fun getMember(key: String): Any? = when (key) {
    ABORTED_PROP -> aborted
    REASON_PROP -> reasonInfo
    THROW_IF_ABORTED_METHOD -> ProxyExecutable { throwIfAborted() }
    else -> null
  }

  override fun hasMember(key: String): Boolean = when (key) {
    ABORTED_PROP,
    REASON_PROP,
    THROW_IF_ABORTED_METHOD -> true
    else -> false
  }

  override fun putMember(key: String?, value: Value?): Unit = Unit

  /** Host-side factory methods. */
  public companion object {
    /**
     * Internal static method to create a new [AbortSignal] instance; meant to be called from `AbortController` only.
     *
     * @return A new empty instance of [AbortSignal].
     */
    public fun create(): AbortSignal = AbortSignal()

    /**
     * Internal static method to create a new [AbortSignal] instance; meant to be called from `AbortController` only.
     *
     * @return A new empty instance of [AbortSignal].
     */
    public fun delegated(iterable: Iterable<AbortSignalAPI>): Abortable {
      val topAborted = java.util.concurrent.atomic.AtomicBoolean(false)
      val abortReason = java.util.concurrent.atomic.AtomicReference<Any?>(null)
      val events = EventAware.create()

      // proxy events to delegates
      events.addEventListener(ABORT_EVENT_NAME) {
        val ev = it.getOrNull(0) as? Event ?: error("Invalid event")
        iterable.forEach { delegated ->
          delegated.dispatchEvent(ev)
        }
      }
      val signal = object: Abortable, EventTarget by events {
        override val aborted: Boolean get() = topAborted.get() || iterable.any { it.aborted }
        override val reason: Any? get() = abortReason.get() ?: iterable.firstOrNull { it.aborted }?.reason
        override fun throwIfAborted(): Unit = iterable.forEach { it.throwIfAborted() }
        override fun assignAborted(reason: Any?) {
          topAborted.compareAndSet(false, true)
          iterable.forEach {
            if (it is Abortable) {
              it.assignAborted(reason)
            }
          }
          if (reason != null) abortReason.set(reason)
          events.dispatchEvent(CustomEvent(ABORT_EVENT_NAME, reason))
        }
      }
      return signal
    }

    /**
     * Internal static method to create a new [AbortSignal] instance which has already been aborted; meant to be called
     * from `AbortController` only.
     *
     * @return A new empty instance of [AbortSignal] which has already been aborted.
     */
    public fun aborted(reason: Any? = null): AbortSignal = AbortSignal(true, reason)

    /**
     * Internal static method to create a new [AbortSignal] instance which is scheduled on the provided guest executor
     * for an automatic expiration.
     *
     * @return A new empty instance of [AbortSignal] which has already been aborted.
     */
    public fun timeout(time: Long, unit: TimeUnit, exec: GuestExecutor): AbortSignal = create().also {
      exec.schedule({ it.assignAborted(TIMED_OUT_EXC) }, time, unit)
    }

    /**
     * Create an instance of the [AbortSignal.Factory], bound to the provided guest executor.
     *
     * @param bound Guest executor to use for abort signal scheduling.
     * @return Abort signal factory.
     */
    public fun factory(bound: GuestExecutor): Factory = Factory.bound(bound)
  }

  // Implements constructor interfaces for `AbortSignal`.
  public class Factory private constructor (private val guestExecutor: GuestExecutor) : AbortSignalAPI.Factory {
    internal companion object {
      @JvmStatic fun bound(guestExecutor: GuestExecutor): Factory = Factory(guestExecutor)
    }

    // Return an abort signal which has already been aborted.
    @Polyglot override fun abort(): AbortSignalAPI = aborted()

    // Return an abort signal which delegates to the state of one or more provided signals.
    @Polyglot override fun any(iterable: Iterable<AbortSignalAPI>): AbortSignalAPI = delegated(iterable)

    // Return an abort signal which aborts after a timeout.
    @Polyglot override fun timeout(time: Int): AbortSignalAPI = timeout(time.toLong(), MILLISECONDS, guestExecutor)

    override fun getMemberKeys(): Array<String> = arrayOf(
      "abort",
      "any",
      "timeout",
    )

    override fun getMember(key: String?): Any? = when(key) {
      "abort" -> ProxyExecutable { abort() }
      "any" -> ProxyExecutable {
        any(it.map { it.`as`<AbortSignalAPI>(AbortSignalAPI::class.java) })
      }
      "timeout" -> ProxyExecutable {
        val first = it.firstOrNull() ?: throw JsError.typeError("First argument must be a number")
        timeout(first.asInt())
      }
      else -> null
    }
  }
}
