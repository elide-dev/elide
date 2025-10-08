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

/**
 * A reference to a pinned [Context][org.graalvm.polyglot.Context] that can be provided to a [ContextAwareExecutor] to
 * request execution only when the context is available.
 *
 * The active context on the current thread can be pinned using [PinnedContext.current] in combination with a
 * [CoroutineGuestContext] or one of the matching [ContextAwareExecutor.execute] overloads.
 *
 * ```kotlin
 * coroutineScope {
 *   // get a reference for the active context, not actually pinned yet
 *   val pinnedContext = PinnedContext.current()
 *   yield()
 *   assertEquals(pinnedContext, PinnedContext.current()) // <- not guaranteed to succeed
 *
 *   withPinnedContext(pinnedContext) { // context is now pinned, use it freely
 *     pinnedContext.value.eval(...) // <- safe
 *     yield()
 *     pinnedContext.value.eval(...) // <- still safe even after suspending
 *   }
 * }
 * ```
 *
 * Note that the underlying [value] is only as safe to use as the one returned by
 * [GuestContext.getCurrent][Context.getCurrent] until it is actually pinned either by submitting a task with
 * [ContextAwareExecutor.execute] or constraining a coroutine context with a [CoroutineGuestContext] element.
 *
 * @see asContextElement
 * @see withPinnedContext
 * @see ContextAwareExecutor
 */
@JvmInline public value class PinnedContext private constructor(public val value: Context) {
  public companion object {
    @ContextAware
    @OptIn(InternalExecutorApi::class)
    public fun current(): PinnedContext = PinnedContext(ContextAwareExecutorBase.pinContextUnsafe())
  }
}
