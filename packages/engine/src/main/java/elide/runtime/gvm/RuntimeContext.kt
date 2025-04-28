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
package elide.runtime.gvm

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import elide.runtime.gvm.internals.MultiThreadedEngine

/**
 * # Runtime Context
 *
 * Offers a public API for obtaining the current [EngineContextAPI] from within an engine thread, and APIs for switching
 * to an exclusive engine-owned execution context.
 *
 * Truffle engines are not always capable of multi-threading: engines like JavaScript must be confined to one execution
 * thread at a time. Elide's execution model involves multiple contexts which are managed by the engine. To facilitate
 * thread safety under this model, a dedicated polyglot [org.graalvm.polyglot.Context] is created for each "engine
 * thread," and that thread entirely owns execution for the duration of its lifetime.
 *
 * Execution tasks are scheduled on the engine thread by enqueueing a task to the "guest executor," which is aware of
 * the engine's execution context.
 */
public object RuntimeContext {
  // Singleton default executor for engine execution.
  private val exec: EngineExecutor by lazy {
    if (System.getProperty("elide.engine.executor") == "sync") {
      TODO("Implement synchronous engine executor.")
    } else {
      MultiThreadedEngine.newMultiThreadedEngineExecutor()
    }
  }

  // Main virtual executor for general I/O-bound tasks.
  private val virtual: ExecutorService by lazy {
    Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())
  }

  // Coroutine dispatcher view into the default engine executor.
  private val engineCoroutineDispatcher: CoroutineDispatcher by lazy {
    exec.asCoroutineDispatcher()
  }

  // Coroutine dispatcher view into the general-purpose virtual executor.
  private val virtualCoroutineDispatcher: CoroutineDispatcher by lazy {
    virtual.asCoroutineDispatcher()
  }

  @Throws(IllegalStateException::class)
  @JvmStatic private fun contextForThread(thread: Thread): EngineContextAPI {
    assert (thread is EngineThreadAPI) {
      "Current thread is not an execution engine thread; cannot obtain context."
    }
    return (thread as EngineThreadAPI).engineContext()
  }

  /**
   * Provide the engine context object for the current thread; if the current thread is not an engine thread with an
   * assigned context, an error is thrown.
   *
   * @throws IllegalStateException If the current thread is not an engine thread.
   * @return The engine context object for the current thread.
   */
  @Throws(IllegalStateException::class)
  @JvmStatic public fun currentUnsafe(): EngineContextAPI = contextForThread(Thread.currentThread())

  /**
   * Indicate whether we are currently executing on an engine thread.
   *
   * @param thread The thread to check; defaults to the current thread.
   * @return `true` if the thread is an engine thread, `false` otherwise.
   */
  @JvmStatic public fun inEngineThread(thread: Thread = Thread.currentThread()): Boolean = thread is EngineThreadAPI

  /**
   * Provide the engine context object for the current thread; if the current thread is not an engine thread with an
   * assigned context, `null` is returned.
   *
   * @throws IllegalStateException If the current thread is not an engine thread.
   * @return The engine context object for the current thread.
   */
  @Throws(IllegalStateException::class)
  @JvmStatic public fun current(): EngineContextAPI? {
    val thread = Thread.currentThread()
    if (thread !is EngineThreadAPI) return null
    return thread.engineContext()
  }

  /**
   * Provide the engine executor, initializing it lazily if needed; the executor can safely accept tasks which are
   * sent to a pool of engine threads.
   *
   * @return Executor interface for the engine.
   */
  @Throws(IllegalStateException::class)
  @JvmStatic public fun engineExecutor(): EngineExecutor {
    return exec
  }

  // Exposes the global engine dispatcher for end-use.
  internal fun globalEngineDispatcher(): CoroutineDispatcher = engineCoroutineDispatcher

  // Exposes the global general-purpose dispatcher for end-use.
  internal fun globalVirtualDispatcher(): CoroutineDispatcher = virtualCoroutineDispatcher
}

/**
 * Obtain a reference to the [CoroutineDispatcher] which corresponds to the active/default [EngineExecutor]; this
 * dispatcher is capable of safely executing guest tasks on dedicated engine threads.
 */
public val Dispatchers.Engine: CoroutineDispatcher get() = RuntimeContext.globalEngineDispatcher()

/**
 * Obtain a reference to the [CoroutineDispatcher] which corresponds to the active/default general-purpose executor;
 * this executor is for general-purpose (especially I/O-bound) tasks.
 */
public val Dispatchers.Virtual: CoroutineDispatcher get() = RuntimeContext.globalVirtualDispatcher()
