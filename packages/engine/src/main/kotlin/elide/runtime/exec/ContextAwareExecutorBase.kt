/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.exec

import org.graalvm.polyglot.Context
import java.util.IdentityHashMap
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.buildList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.concurrent.withLock

/**
 * Delegating [ContextAwareExecutor] implementation backed by an [ExecutorService] that dispatches the tasks, and with
 * a lazily growing bounded context pool.
 *
 * The [ContextAwareExecutor] function can be used to create instances of this class.
 */
@InternalExecutorApi public abstract class ContextAwareExecutorBase(
  private val maxContextPoolSize: Int,
  private val baseExecutor: ExecutorService,
) : AbstractExecutorService(), ContextAwareExecutor {
  private class CompletableFutureTask<T>(callable: Callable<T>) : FutureTask<T>(callable) {
    constructor(runnable: Runnable, result: T) : this(Executors.callable(runnable, result))

    fun setFailed(cause: Throwable?) {
      setException(cause)
    }

    fun setSuccess(result: T) {
      set(result)
    }
  }

  /** State holder for a [context] in the executor's pool. The [locals] map holds the values for [ContextLocal]s. */
  private class ContextHolder {
    lateinit var context: Context
    val locals: MutableMap<Any, Any?> = mutableMapOf()
  }

  /** A pool of free contexts that can be used for dispatch. */
  private val contextPool = ArrayDeque<ContextHolder>()

  /** A list of pending tasks that can run on any context. */
  private val pendingUnconfined = ArrayDeque<Runnable>()

  /** Total number of [pendingConfined] tasks. */
  private var pendingConfinedCount = 0

  /** Map holding pending tasks that require a specific context for dispatch. */
  private val pendingConfined = IdentityHashMap<Context, ArrayDeque<Runnable>>()

  /** The total number of contexts added to the pool, both in-use and free. */
  private var contextPoolSize = 0

  /** Lock guarding all state in the executor. */
  private val lock = ReentrantLock()

  override val onDispatchThread: Boolean
    get() = currentContextHolder.get() != null

  /**
   * Produce a new [Context] to be added to the internal pool. This method is called when the pool needs to be
   * expanded and should be used by subclasses to configure and initialize a context before it is used for dispatch.
   */
  protected abstract fun newContext(): Context

  // context-aware methods ------------------------------------------------------------------------

  @ContextAware override fun <T> setContextLocal(contextLocal: ContextLocal<T>, value: T) {
    requireActiveHolder().locals[contextLocal] = value
  }

  @ContextAware override fun clearContextLocal(contextLocal: ContextLocal<*>) {
    requireActiveHolder().locals.remove(contextLocal)
  }

  // executor lifecycle methods -------------------------------------------------------------------

  override fun isShutdown(): Boolean = baseExecutor.isShutdown
  override fun isTerminated(): Boolean = baseExecutor.isTerminated
  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = baseExecutor.awaitTermination(timeout, unit)

  override fun shutdown() {
    lock.withLock {
      pendingConfined.keys.forEach { it.close(true) }
      contextPool.forEach { it.context.close(true) }
      contextPool.clear()
    }
    baseExecutor.shutdown()
  }

  override fun shutdownNow(): List<Runnable?> {
    return buildList {
      addAll(baseExecutor.shutdownNow())
      addAll(pendingUnconfined)

      lock.withLock {
        pendingConfined.forEach { (context, runnables) ->
          addAll(runnables)
          context.close(true)
        }

        contextPool.forEach { it.context.close(true) }
        contextPool.clear()
      }
    }
  }

  // plain executor task submissions (no context) -------------------------------------------------

  override fun executeDirect(command: Runnable): Unit = baseExecutor.execute(command)
  override fun <T> submitDirect(task: Callable<T>): Future<T> = baseExecutor.submit(task)
  override fun <T> submitDirect(task: Runnable, result: T): Future<T> = baseExecutor.submit(task, result)

  // base executor task submissions (unconfined) --------------------------------------------------

  override fun execute(command: Runnable) {
    enqueue(command)
  }

  override fun submit(task: Runnable): Future<*> = enqueue(task, Unit)
  override fun <T> submit(task: Callable<T?>): Future<T?> = enqueue(task)
  override fun <T> submit(task: Runnable, result: T?): Future<T?> = enqueue(task, result)

  private fun <T> enqueue(callable: Callable<T>): Future<T> = enqueue(CompletableFutureTask(callable))
  private fun <T> enqueue(runnable: Runnable, result: T): Future<T> = enqueue(CompletableFutureTask(runnable, result))

  private fun <T : Runnable> enqueue(task: T): T {
    lock.withLock {
      pendingUnconfined.add(task)
      drainLocked()
    }

    return task
  }

  // context-aware task submissions (confined) ----------------------------------------------------

  override fun execute(context: PinnedContext, command: Runnable) {
    enqueueConfined(command, context)
  }

  override fun submit(context: PinnedContext, task: Runnable): Future<*> = enqueueConfined(task, Unit, context)
  override fun <T> submit(context: PinnedContext, task: Callable<T>): Future<T> = enqueueConfined(task, context)
  override fun <T> submit(context: PinnedContext, task: Runnable, result: T): Future<T> =
    enqueueConfined(task, result, context)

  private fun <T> enqueueConfined(callable: Callable<T>, pin: PinnedContext): Future<T> {
    return enqueueConfined(CompletableFutureTask(callable), pin)
  }

  private fun <T> enqueueConfined(runnable: Runnable, result: T, pin: PinnedContext): Future<T> {
    return enqueueConfined(CompletableFutureTask(runnable, result), pin)
  }

  // scheduling details ---------------------------------------------------------------------------

  private fun <T : Runnable> enqueueConfined(task: T, pin: PinnedContext): T {
    lock.withLock {
      pendingConfined.getOrPut(pin.value) { ArrayDeque() }.add(task)

      pendingConfinedCount++
      drainLocked()
    }

    return task
  }

  private fun drainLocked() {
    // dispatch confined tasks first
    with(contextPool.iterator()) {
      while (pendingConfinedCount > 0 && hasNext()) {
        val holder = next()
        val task = pendingConfined.getOrPut(holder.context) { ArrayDeque() }
          .removeFirstOrNull()
          ?: continue

        pendingConfinedCount--
        runTaskInContext(task, holder)
        remove() // remove context from the pool
      }
    }

    // dispatch unconfined tasks using the remainder of the pool
    while (contextPool.isNotEmpty()) {
      val holder = contextPool.first() // don't remove yet

      // prioritize confined tasks
      val task = pendingUnconfined.removeFirstOrNull() ?: break

      contextPool.removeFirst() // remove it now it's being used
      runTaskInContext(task, holder)
    }

    // if we still have pending tasks, expand the pool within bounds
    while (pendingUnconfined.isNotEmpty() && contextPoolSize < maxContextPoolSize) {
      contextPoolSize++

      val task = pendingUnconfined.removeFirst()
      runTaskInContext(task)
    }
  }

  private fun runTaskInContext(task: Runnable, holder: ContextHolder? = null) = baseExecutor.execute {
    val effectiveHolder = holder ?: ContextHolder()
    var initialized = holder != null

    currentContextHolder.set(effectiveHolder)
    ContextLocalImpl.attach(effectiveHolder.locals)

    val result = runCatching {
      if (!initialized) effectiveHolder.context = newContext()
      effectiveHolder.context.enter()

      initialized = true
      task.run()
    }

    if (initialized) effectiveHolder.context.leave()

    currentContextHolder.remove()
    ContextLocalImpl.detach()

    lock.withLock {
      if (initialized) contextPool.addLast(effectiveHolder)
      drainLocked()
    }

    if (result.isFailure && task is CompletableFutureTask<*>) task.setFailed(result.exceptionOrNull())
    else result.getOrThrow()
  }

  public companion object {
    private val currentContextHolder = ThreadLocal<ContextHolder>()

    @ContextAware private fun requireActiveHolder(): ContextHolder {
      return currentContextHolder.get() ?: throw NoActiveContextError()
    }

    @ContextAware
    @InternalExecutorApi internal fun pinContextUnsafe(): Context = requireActiveHolder().context
  }
}
