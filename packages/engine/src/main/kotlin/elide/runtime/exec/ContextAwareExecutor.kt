/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.exec

import org.graalvm.polyglot.Context
import java.util.IdentityHashMap
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.collections.ArrayDeque
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.buildList
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import elide.runtime.core.PolyglotEngine

public class ContextAwareExecutor(
  private val maxContextPoolSize: Int,
  private val baseExecutor: ExecutorService,
  private val contextFactory: () -> Context,
) : AbstractExecutorService() {
  private class ContextHolder(
    val context: Context,
    val contextLocals: MutableMap<Long, Any> = mutableMapOf(),
  )

  public inner class ContextLocal<T> {
    private val handle: Long = nextContextLocalId.getAndIncrement()

    public val bound: Boolean
      get() = currentContextHolder.get() != null

    public fun get(): T? {
      @Suppress("UNCHECKED_CAST")
      return currentContextHolder.get()?.contextLocals?.get(handle) as T?
    }

    public fun set(value: T) {
      currentContextHolder.get()?.contextLocals?.set(handle, value as Any)
        ?: error("No active context, context-local value can only be set within a context")
    }

    public fun clear() {
      currentContextHolder.get()?.contextLocals?.remove(handle)
    }
  }

  private val nextContextLocalId = AtomicLong()

  private val contextPool = ArrayDeque<ContextHolder>()

  private val pendingUnbounded = ArrayDeque<Runnable>()

  private val pendingBounded = IdentityHashMap<Context, ArrayDeque<Runnable>>()

  private val currentContextHolder = ThreadLocal<ContextHolder>()

  private var contextPoolSize = 0

  private val lock = ReentrantLock()

  public val onDispatchThread: Boolean
    get() = currentContextHolder.get() != null

  public fun pinContext(): Context? = currentContextHolder.get()?.context

  override fun isShutdown(): Boolean = baseExecutor.isShutdown
  override fun isTerminated(): Boolean = baseExecutor.isTerminated
  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = baseExecutor.awaitTermination(timeout, unit)

  override fun shutdown(): Unit = baseExecutor.shutdown()

  override fun shutdownNow(): List<Runnable?> {
    return buildList {
      addAll(baseExecutor.shutdownNow())
      addAll(pendingUnbounded)
      pendingBounded.values.forEach(::addAll)
    }
  }

  override fun execute(command: Runnable) {
    enqueue(command)
  }

  override fun submit(task: Runnable): Future<*> = enqueue(task, Unit)
  override fun <T> submit(task: Callable<T?>): Future<T?> = enqueue(task)
  override fun <T> submit(task: Runnable, result: T?): Future<T?> = enqueue(task, result)

  public fun execute(context: Context, command: Runnable) {
    enqueueBounded(command, context)
  }

  public fun submit(context: Context, task: Runnable): Future<*> = enqueueBounded(task, Unit, context)
  public fun <T> submit(context: Context, task: Callable<T>): Future<T> = enqueueBounded(task, context)
  public fun <T> submit(context: Context, task: Runnable, result: T): Future<T> = enqueueBounded(task, result, context)

  private fun <T> enqueue(callable: Callable<T>): Future<T> = enqueue(FutureTask(callable))
  private fun <T> enqueue(runnable: Runnable, result: T): Future<T> = enqueue(FutureTask(runnable, result))

  private fun <T : Runnable> enqueue(task: T): T {
    lock.withLock {
      pendingUnbounded.add(task)
      drainLocked()
    }

    return task
  }

  private fun <T> enqueueBounded(callable: Callable<T>, context: Context): Future<T> {
    return enqueueBounded(FutureTask(callable), context)
  }

  private fun <T> enqueueBounded(runnable: Runnable, result: T, context: Context): Future<T> {
    return enqueueBounded(FutureTask(runnable, result), context)
  }

  private fun <T : Runnable> enqueueBounded(task: T, context: Context): T {
    lock.withLock {
      pendingBounded[context]?.add(task)
        ?: throw IllegalArgumentException("Context $context is not part of this executor's pool")

      drainLocked()
    }

    return task
  }

  private fun drainLocked() {
    while (contextPool.isNotEmpty()) {
      val holder = contextPool.first() // don't remove yet

      // prioritize bounded tasks
      val task = pendingBounded[holder.context]!!.removeFirstOrNull()
        ?: pendingUnbounded.removeFirstOrNull()
        ?: break

      contextPool.removeFirst() // remove it now it's being used
      runTaskInContext(task, holder)
    }

    // if we still have pending tasks, expand the pool within bounds
    while (pendingUnbounded.isNotEmpty() && contextPoolSize < maxContextPoolSize) {
      val context = contextFactory()
      pendingBounded[context] = ArrayDeque()
      contextPoolSize++

      val task = pendingUnbounded.removeFirst()
      runTaskInContext(task, ContextHolder(context))
    }
  }

  private fun runTaskInContext(task: Runnable, holder: ContextHolder) = baseExecutor.execute {
    val context = holder.context
    currentContextHolder.set(holder)
    context.enter()
    try {
      task.run()
    } finally {
      context.leave()
      currentContextHolder.remove()

      lock.withLock {
        contextPool.addLast(holder)
        drainLocked()
      }
    }
  }

  public companion object {
    @JvmStatic public fun forEngine(
      engine: PolyglotEngine,
      threadPoolSize: Int = Runtime.getRuntime().availableProcessors(),
      contextPoolSize: Int = threadPoolSize,
    ): ContextAwareExecutor {
      return forEngine(engine, Executors.newScheduledThreadPool(threadPoolSize), contextPoolSize)
    }

    @JvmStatic public fun forEngine(
      engine: PolyglotEngine,
      baseExecutor: ExecutorService,
      contextPoolSize: Int = Runtime.getRuntime().availableProcessors(),
    ): ContextAwareExecutor {
      return ContextAwareExecutor(contextPoolSize, baseExecutor) { engine.acquire().unwrap() }
    }
  }
}
