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
@file:Suppress("TooGenericExceptionCaught", "NOTHING_TO_INLINE")

package elide.runtime.gvm.internals.js

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.Closeable
import java.lang.ref.WeakReference
import java.time.Duration
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.TimerId
import elide.runtime.intrinsics.js.Timers

// Symbol where `setTimeout` is expected.
private const val SET_TIMEOUT_SYMBOL = "setTimeout"

// Symbol where `setInterval` is expected.
private const val SET_INTERVAL_SYMBOL = "setInterval"

// Symbol where `clearTimeout` is expected.
private const val CLEAR_TIMEOUT_SYMBOL = "clearTimeout"

// Symbol where `clearInterval` is expected.
private const val CLEAR_INTERVAL_SYMBOL = "clearInterval"

// Number of threads to run for timer execution.
private const val TIMER_POOL_THREADS = 1

// Default delay for timers.
private const val DEFAULT_DELAY: Long = 0L

// Milliseconds to wait for termination.
private const val SHUTDOWN_WAIT: Long = 10L

/**
 * JavaScript Timer Executor Provider
 *
 * Provides a scheduled executor service for JavaScript timers.
 */
@FunctionalInterface internal fun interface JsTimerExecutorProvider {
  fun provide(): ListeningScheduledExecutorService
}

// Implements JavaScript timer execution with a listening scheduled executor service.
@Factory internal class JsTimerExecutorProviderImpl : JsTimerExecutorProvider {
  @Singleton override fun provide(): ListeningScheduledExecutorService =
    MoreExecutors.listeningDecorator(
      MoreExecutors.getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor(TIMER_POOL_THREADS, Thread
          .ofPlatform()
          .name("elide-js-timer")
          .factory())
      )
    )
}

// Mounts JavaScript timer intrinsics.
@Intrinsic @Factory internal class JsTimersIntrinsic @Inject constructor (
  private val execProvider: JsTimerExecutorProvider,
) : AbstractJsIntrinsic() {
  @Singleton fun provide(): Timers = manager

  private val manager: JsTimerManager by lazy {
    JsTimerManager(execProvider.provide())
  }

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: MutableIntrinsicBindings) {
    // `setTimeout`
    bindings[SET_TIMEOUT_SYMBOL.asPublicJsSymbol()] = ProxyExecutable {
      val cbk = it.getOrNull(0)
      val delay = it.getOrNull(1)?.asLong() ?: DEFAULT_DELAY
      manager.setTimeout(delay) { cbk?.executeVoid() }
    }

    // `setInterval`
    bindings[SET_INTERVAL_SYMBOL.asPublicJsSymbol()] = ProxyExecutable {
      val cbk = it.getOrNull(0)
      val delay = it.getOrNull(1)?.asLong() ?: DEFAULT_DELAY
      manager.setInterval(delay) { cbk?.executeVoid() }
    }

    // `clearTimeout`
    bindings[CLEAR_TIMEOUT_SYMBOL.asPublicJsSymbol()] = ProxyExecutable {
      val id = it.getOrNull(0)?.asLong() ?: return@ProxyExecutable null
      manager.clearTimeout(id)
    }

    // `clearInterval`
    bindings[CLEAR_INTERVAL_SYMBOL.asPublicJsSymbol()] = ProxyExecutable {
      val id = it.getOrNull(0)?.asLong() ?: return@ProxyExecutable null
      manager.clearInterval(id)
    }
  }
}

/**
 * # JavaScript Timer
 *
 * Implements JavaScript intrinsic timer functionality, both for one-shot timers and repeated (interval) timers. Timers
 * are created by the [JsTimerManager], which has symbols mounted at `setTimeout` and `setInterval`.
 *
 * No retention of JavaScript timers is performed until a timer is scheduled. When a timer is scheduled, it is retained
 * until either it is canceled or it is executed (in the case of one-shot timers). If a timer is a repeating timer,
 * it is retained until it is canceled without condition.
 *
 * &nbsp;
 *
 * ## Timer IDs & Cancellation
 *
 * Timers are always assigned a [Long] ID when created; this ID can be used to cancel the timer. Canceling a one-shot
 * timer before it executes will prevent it from executing; canceling a repeating timer will prevent it from executing
 * again.
 *
 * If a timer has already been canceled, or cancellation would otherwise have no effect anyway (for example, canceling a
 * one-shot timer that has executed), the cancel operation is a no-op.
 *
 * &nbsp;
 *
 * ## Error Handling
 *
 * Errors do not propagate from the executed timer callbacks; an optional [errHandler] can be provided which receives
 * instances of [Throwable]. Because timers are executed from background threads, it is important to handle exceptions
 * from within user code, because they cannot be surfaced to the JavaScript runtime.
 */
@Suppress("LongParameterList")
internal class JsTimer private constructor (
  private val timerId: TimerId,
  isScheduled: Boolean = false,
  private val repeat: Boolean,
  @Suppress("unused") private val ctx: Context,
  private val timers: WeakReference<MutableMap<TimerId, JsTimer>>,
  private val exec: ListeningScheduledExecutorService,
  private val delay: Duration,
  private val callback: () -> Unit,
  private val errHandler: (Throwable) -> Unit,
) : Runnable, Closeable, AutoCloseable {
  // Whether this timer is enabled; flipped to `false` if the timer is canceled.
  private val enabled: AtomicBoolean = AtomicBoolean(true)

  // Whether this timer is scheduled.
  private val scheduled: AtomicBoolean = AtomicBoolean(isScheduled)

  // Whether this timer is scheduled.
  private val future: AtomicReference<ListenableFuture<*>> = AtomicReference(null)

  // Number of times this timer has executed.
  private val executions = atomic(0L)

  /** Utilities for creating raw JavaScript timer objects. */
  internal companion object {
    /**
     * Creates a new timer from scratch.
     *
     * @param id The timer ID.
     * @param exec The executor service to use.
     * @param delay The delay before the timer fires.
     * @param callback The callback to execute.
     * @param repeat Whether the timer should repeat.
     * @param errHandler The error handler to use.
     * @return The new timer.
     */
    @JvmStatic fun create(
      id: TimerId,
      timers: MutableMap<TimerId, JsTimer>,
      ctx: Context,
      exec: ListeningScheduledExecutorService,
      delay: Long,
      callback: () -> Unit,
      repeat: Boolean,
      errHandler: (Throwable) -> Unit = { it.printStackTrace() }
    ): JsTimer =
      JsTimer(
        id,
        isScheduled = false,
        repeat = repeat,
        exec = exec,
        timers = WeakReference(timers),
        ctx = ctx,
        delay = Duration.ofMillis(delay),
        callback = callback,
        errHandler = errHandler,
      )

    /**
     * Create a one-shot timer.
     *
     * @param id ID value to use.
     * @param exec The executor service to use.
     * @param delay The delay before the timer fires.
     * @param callback The callback to execute.
     * @return The new timer.
     */
    @JvmStatic inline fun oneshot(
      id: Long,
      timers: MutableMap<TimerId, JsTimer>,
      exec: ListeningScheduledExecutorService,
      delay: Long? = null,
      ctx: Context = Context.getCurrent(),
      noinline callback: () -> Unit,
    ) = create(
      id,
      timers,
      ctx,
      exec,
      delay ?: DEFAULT_DELAY,
      callback,
      repeat = false,
    ).also {
      require(it.timerId !in timers)
      timers[it.timerId] = it
    }.schedule()

    /**
     * Create a timer which re-schedules itself.
     *
     * @param id ID value to use.
     * @param exec The executor service to use.
     * @param delay The delay before the timer fires.
     * @param callback The callback to execute.
     * @return The new timer.
     */
    @JvmStatic inline fun repeated(
      id: Long,
      timers: MutableMap<TimerId, JsTimer>,
      exec: ListeningScheduledExecutorService,
      delay: Long? = null,
      ctx: Context = Context.getCurrent(),
      noinline callback: () -> Unit,
    ) = create(
      id,
      timers,
      ctx,
      exec,
      delay ?: DEFAULT_DELAY,
      callback,
      repeat = true,
    ).also {
      require(it.timerId !in timers)
      timers[it.timerId] = it
    }.schedule()
  }

  // ID of this timer.
  val id: TimerId get() = timerId

  // Schedule the timer for execution, in effect "starting" the timer.
  fun schedule(): TimerId {
    require(!scheduled.get()) { "Timer must not be scheduled before scheduling" }
    scheduled.set(true)
    doSchedule()
    return id
  }

  // Cancel the timer before the next execution.
  fun cancel() {
    enabled.set(false)
  }

  // Actually schedule the timer for execution.
  private fun doSchedule() {
    future.set(exec.schedule(
      this,
      delay,
    ))
  }

  // If the timer is due to repeat, schedule the next execution.
  private fun maybeSchedule() {
    if (enabled.get()) doSchedule()
  }

  override fun run() {
    require(scheduled.get()) { "Timer must be scheduled before execution" }

    if (enabled.get()) try {
      callback()
    } catch (e: Throwable) {
      // Log the error.
      errHandler.invoke(e)
    } finally {
      executions.incrementAndGet()

      if (repeat) {
        // Reschedule the timer.
        maybeSchedule()
      } else {
        // Mark the timer as executed.
        close()
      }
    } else {
      // timer is no longer enabled; close/cancel it
      close()
    }
  }

  override fun close() {
    if (enabled.get()) cancel()
    scheduled.set(false)
    timers.get()?.remove(timerId)
  }
}

/**
 * # JavaScript Timer Manager
 *
 * Central management and dispatch of all JavaScript intrinsic timers, including one-shot timers and repeated timers, as
 * scheduled via [setTimeout] and [setInterval].
 *
 * Timers are executed on a background thread, and are dispatched by a [ListeningScheduledExecutorService]. This service
 * is provided by the [JsTimerExecutorProvider] and is created with a pool of platform threads.
 */
internal class JsTimerManager (private val exec: ListeningScheduledExecutorService) : Timers, Closeable, AutoCloseable {
  private val counter: AtomicLong = atomic(0L)
  private val timers: MutableMap<TimerId, JsTimer> = ConcurrentSkipListMap()

  override fun setTimeout(delay: Long?, callback: () -> Unit): TimerId = JsTimer.oneshot(
    counter.getAndIncrement(),
    timers,
    exec,
    delay,
    callback = callback,
  )

  override fun setTimeout(delay: Long?, vararg arg: Any?, callback: Value): TimerId = JsTimer.repeated(
    counter.getAndIncrement(),
    timers,
    exec,
    delay,
  ) {
    if (arg.isNotEmpty()) {
      callback.execute(*arg)
    } else {
      callback.executeVoid()
    }
  }

  override fun setInterval(delay: Long?, callback: () -> Unit): TimerId = JsTimer.repeated(
    counter.getAndIncrement(),
    timers,
    exec,
    delay,
    callback = callback,
  )

  override fun setInterval(delay: Long?, vararg arg: Any?, callback: Value): TimerId = JsTimer.repeated(
    counter.getAndIncrement(),
    timers,
    exec,
    delay,
  ) {
    if (arg.isNotEmpty()) {
      callback.execute(*arg)
    } else {
      callback.executeVoid()
    }
  }

  override fun clearTimeout(id: TimerId) {
    timers[id]?.close()
  }

  override fun clearInterval(id: TimerId) {
    timers[id]?.close()
  }

  override fun close() {
    try {
      exec.shutdownNow()
      exec.awaitTermination(SHUTDOWN_WAIT, MILLISECONDS)
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
    }
  }
}
