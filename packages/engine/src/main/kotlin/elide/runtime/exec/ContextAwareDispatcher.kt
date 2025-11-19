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
package elide.runtime.exec

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Returns a [CoroutineGuestContext.Pinned] element that pins coroutines to this context, dispatching them only when
 * the context is available regardless of the thread they are in.
 *
 * This element can be used to guarantee a stable active context within a coroutine after a suspension point.
 *
 * @see CoroutineGuestContext.Pinned
 * @see withPinnedContext
 */
public fun PinnedContext.asContextElement(): CoroutineContext = CoroutineGuestContext.Pinned(this)

/**
 * Run the specified [block] with a [pinned] guest context, guaranteeing the same context will be active in the
 * dispatching thread after suspensions and in all child coroutines.
 *
 * @see CoroutineGuestContext.Pinned
 * @see asContextElement
 */
public suspend inline fun <T> withPinnedContext(
  pinned: PinnedContext = PinnedContext.current(),
  context: CoroutineContext = EmptyCoroutineContext,
  crossinline block: suspend CoroutineScope.() -> T
): T {
  return withContext(context + pinned.asContextElement()) { block() }
}

public sealed interface CoroutineGuestContext : CoroutineContext.Element {
  /**
   * Pins coroutine dispatch to a specific guest context, preventing execution when the context is in use even if there
   * are available threads that could resume the coroutine. A typical use of this element is to guarantee the same guest
   * context instance will be active after a suspension point:
   *
   * ```kotlin
   * coroutineScope(PinnedContext.current().asContextElement()) {
   *   // store a context-bound value
   *   val fn = GuestContext.getCurrent().eval(JAVA_SCRIPT, "() => 42")
   *
   *   yield() // suspend
   *   fn.execute() // this is safe, the context is preserved after suspension
   * }
   * ```
   *
   * It is possible to unpin the context by passing [CoroutineGuestContext.Unpinned] in a [withContext] block.
   *
   * @see Unpinned
   * @see None
   */
  @JvmInline public value class Pinned(public val pin: PinnedContext) : CoroutineGuestContext

  /**
   * Unpins the coroutine context, allowing child coroutines to be dispatched with any guest context. This value is
   * useful to allow concurrency within blocks that have been pinned using a [CoroutineGuestContext.Pinned].
   *
   * ```kotlin
   * coroutineScope(PinnedContext.current().asContextElement()) {
   *   launch { GuestContext.getCurrent().eval(...) }
   *
   *   withContext(CoroutineGuestContext.Unpinned) {
   *     launch {
   *       // this block may receive a different context than its parent
   *       GuestContext.getCurrent().eval(...)
   *     }
   *   }
   * }
   * ```
   *
   * @see Pinned
   * @see None
   */
  public data object Unpinned : CoroutineGuestContext

  /**
   * Releases the current [Context][org.graalvm.polyglot.Context], allowing it to be used by other threads. This
   * element can be used to launch background work that does not require any access to context-aware APIs.
   *
   * ```kotlin
   * coroutineScope(PinnedContext.current().asContextElement()) {
   *   launch { GuestContext.getCurrent().eval(...) }
   *
   *   withContext(CoroutineGuestContext.None) {
   *     // bad: this block does not have an active context
   *     // GuestContext.getCurrent().eval(...)
   *
   *     // good: run arbitrary background tasks that may block
   *     File(...).readText()
   *   }
   * }
   * ```
   *
   * @see Pinned
   * @see Unpinned
   */
  public data object None : CoroutineGuestContext

  override val key: CoroutineContext.Key<*> get() = Key

  public companion object Key : CoroutineContext.Key<CoroutineGuestContext> {
    /**
     * Pins the current active context and returns it as a context element. Equivalent to
     * [PinnedContext.asContextElement] or `CoroutineGuestContext.Pinned(PinnedContext.current())`.
     */
    public fun pinCurrent(): Pinned = Pinned(PinnedContext.current())
  }
}

/**
 * A [Context][org.graalvm.polyglot.Context]-aware coroutine dispatcher that allows for
 * [pinning][CoroutineGuestContext.Pinned] execution. Use [ContextAwareExecutor].[asContextAwareDispatcher] to create
 * an instance (or pass the dispatcher in the constructor).
 *
 * ```kotlin
 * // note the use of a custom extension when converting
 * val dispatcher = ContextAwareExecutor(...).asContextAwareDispatcher()
 * val scope = CoroutineScope(dispatcher + SupervisorJob())
 *
 * scope.launch {
 *   // an entered context is always available
 *   GuestContext.getCurrent().eval(GuestLanguages.JAVA_SCRIPT, "console.log('hello');")
 * }
 * ```
 *
 * Concurrent coroutines may still be dispatched using the same guest context on different threads, but they are
 * guaranteed to never execute concurrently in that case (thus avoiding concurrent access limitations).
 *
 * ### Context pinning
 *
 * Use [withPinnedContext] or [PinnedContext.asContextElement] to ensure a coroutine and its child jobs are always
 * dispatched with the same context, even after suspension points.
 *
 * ```kotlin
 * withContext(PinnedContext.current().asContextElement()) {
 *   val fn = GuestContext.getCurrent().eval(GuestLanguages.JAVA_SCRIPT, "() => 42")
 *   yield()
 *   val result = fn.execute() // safe to call
 *
 *   withContext(CoroutineGuestContext.Unpinned) {
 *     // run guest-aware code with an arbitrary pooled context
 *   }
 *
 *   withContext(CoroutineGuestContext.None) {
 *     // run arbitrary background work without capturing a context
 *   }
 * }
 * ```
 *
 * @see withPinnedContext
 * @see asContextAwareDispatcher
 */
public class ContextAwareDispatcher(override val executor: ContextAwareExecutor) : ExecutorCoroutineDispatcher() {
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    when (val current = context[CoroutineGuestContext] ?: CoroutineGuestContext.Unpinned) {
      is CoroutineGuestContext.Pinned -> executor.execute(current.pin, block)
      CoroutineGuestContext.Unpinned -> executor.execute(block)
      CoroutineGuestContext.None -> executor.executeDirect(block)
    }
  }

  override fun close() {
    executor.close()
  }
}

/**
 * Returns a [ContextAwareDispatcher] backed by this executor, allowing the use of [CoroutineGuestContext]
 * elements.
 *
 * This extension should always be used in place of [asCoroutineDispatcher] to ensure all context-aware features are
 * available.
 *
 * The dispatcher owns the underlying executor and must be manually [closed][ContextAwareDispatcher.close] to release
 * resources when it is no longer necessary.
 */
public fun ContextAwareExecutor.asContextAwareDispatcher(): ContextAwareDispatcher = ContextAwareDispatcher(this)
