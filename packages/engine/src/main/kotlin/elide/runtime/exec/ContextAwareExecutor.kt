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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlinx.coroutines.asCoroutineDispatcher
import elide.runtime.core.InternalRuntimeException

/**
 * Denotes a symbol as part of the internal [ContextAwareExecutor] API.
 *
 * Symbols with this annotation **must not** be used in general code: breaking this contract can potentially crash the
 * executor, leaving orphaned tasks or causing deadlocks.
 *
 * The intent of this annotation is to allow optimizations to be used by general code when they should only be created
 * or updated by the executor.
 */
@RequiresOptIn("This symbol is meant for use only by a ContextAwareExecutor. Do *not* use it in general code")
public annotation class InternalExecutorApi

/** Thrown when a [ContextAware] API is called without an active [Context][org.graalvm.polyglot.Context]. */
public class NoActiveContextError(expected: String? = null) : InternalRuntimeException(
  "A context-aware API was called but no context is active" + expected?.let { ": $it" }.orEmpty(),
)

/**
 * An [ExecutorService] that keeps an internal pool of [Context][org.graalvm.polyglot.Context] and dispatches tasks
 * only when a context is not in use.
 *
 * Tasks dispatched by this executor service can access [ContextAware] APIs such as [ContextLocal] values. They also
 * have automatic access to an entered [Context][org.graalvm.polyglot.Context] on the dispatched thread that can be
 * used without risk of concurrent access.
 *
 * ```kotlin
 * val executor = ContextAwareExecutor(
 *   maxContextPoolSize = 5,
 *   baseExecutor = Executors.newFixedThreadPool(10),
 *   contextFactory = { ... }
 * )
 *
 * val LocalMessage = ContextLocal<String>()
 *
 * val usedContext = executor.submit {
 *   // an entered context is always available
 *   GuestContext.getCurrent().eval(GuestLanguages.JAVA_SCRIPT, "console.log('hello');")
 *   executor.setContextLocal(LocalMessage, "hello")
 * }
 *
 * // pin tasks to a context to avoid concurrent access to it while allowing multithreading
 * executor.submit(usedContext) {
 *   val message = LocalMessage.current() // returns "hello"
 *   GuestContext.getCurrent().eval(GuestLanguages.JAVA_SCRIPT, "console.log('$message');")
 * }
 * ```
 *
 * For generic background tasks that do not require a guest context, use [executeDirect] or [submitDirect]; this will
 * dispatch the task without capturing a context, allowing other threads to use the pool instead:
 *
 * ```
 * executor.executeDirect {
 *   // no context available for this task, use it to run background work
 *   File("./hello.txt").writeText("Hello World!")
 * }
 * ```
 *
 * This service can be used to run coroutines when converted to a dispatcher with [asContextAwareDispatcher]. Note the
 * custom extension that allows context pinning; using the standard [asCoroutineDispatcher] extension will work, but
 * dispatched coroutines will not be able to pin a guest context using [PinnedContext] elements.
 *
 * @see ContextAwareDispatcher
 */
public interface ContextAwareExecutor : ExecutorService {
  /**
   * Returns `true` if the current thread is being dispatched by the executor and [ContextAware] APIs can be safely
   * called.
   */
  public val onDispatchThread: Boolean

  /**
   * Sets the [value] of a [contextLocal] for the active context. The value can then be accessed by tasks dispatched
   * in that specific context regardless of the thread.
   *
   * @see clearContextLocal
   */
  @ContextAware public fun <T> setContextLocal(contextLocal: ContextLocal<T>, value: T)

  /**
   * Clears the value of a [contextLocal] if present for the active context. Accessing the context local after this
   * call will return `null`.
   *
   * @see setContextLocal
   */
  @ContextAware public fun clearContextLocal(contextLocal: ContextLocal<*>)

  /** Execute the provided [command] with a pinned [context]. The task will run only when the context is not in use. */
  public fun execute(context: PinnedContext, command: Runnable)

  /** Execute the given [task] with a pinned [context]. The task will run only when the context is not in use. */
  public fun submit(context: PinnedContext, task: Runnable): Future<*>

  /** Execute the given [task] with a pinned [context]. The task will run only when the context is not in use. */
  public fun <T> submit(context: PinnedContext, task: Callable<T>): Future<T>

  /** Execute the given [task] with a pinned [context]. The task will run only when the context is not in use. */
  public fun <T> submit(context: PinnedContext, task: Runnable, result: T): Future<T>

  /**
   * Execute the given [command] **without** acquiring a [Context][org.graalvm.polyglot.Context]. The task will not
   * have access to context-aware APIs, so this variant is better suited for background work that doesn't interact with
   * guest code.
   */
  public fun executeDirect(command: Runnable)

  /**
   * Execute the given [task] **without** acquiring a [Context][org.graalvm.polyglot.Context] and track its progress.
   * The task will not have access to context-aware APIs, so this variant is better suited for background work that
   * doesn't interact with guest code.
   */
  public fun <T> submitDirect(task: Callable<T>): Future<T>

  /**
   * Execute the given [task] **without** acquiring a [Context][org.graalvm.polyglot.Context] and track its progress.
   * The task will not have access to context-aware APIs, so this variant is better suited for background work that
   * doesn't interact with guest code.
   */
  public fun <T> submitDirect(task: Runnable, result: T): Future<T>
}

/**
 * Returns a new [ContextAwareExecutor] that uses a [baseExecutor] to schedule tasks.
 *
 * New context instances are added to the pool when needed using the [contextFactory] until the [maxContextPoolSize]
 * is reached.
 */
@OptIn(InternalExecutorApi::class)
public fun ContextAwareExecutor(
  maxContextPoolSize: Int,
  baseExecutor: ExecutorService,
  contextFactory: () -> Context,
): ContextAwareExecutor {
  return object : ContextAwareExecutorBase(maxContextPoolSize, baseExecutor) {
    override fun newContext(): Context = contextFactory()
  }
}
