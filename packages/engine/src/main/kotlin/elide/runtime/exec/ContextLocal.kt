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

/** Alias for untyped mutable maps used to store context local values. */
private typealias UnsafeContextLocalMap = MutableMap<Any, Any?>

/**
 * A value bound to a specific [Context][org.graalvm.polyglot.Context] instance, available within code dispatched by a
 * [ContextAwareExecutor].
 *
 * Context local values can only be set by calling [ContextAwareExecutor.setContextLocal], but they can be read freely
 * using [ContextLocal.current] as long as the calling thread has an active context.
 *
 * ```kotlin
 * val LocalMessage = ContextLocal<String>()
 * val executor = ContextAwareExecutor(...)
 *
 * val pin = executor.submit {
 *   // set the value for the current context
 *   executor.setContextLocal(LocalMessage, "hello")
 *   val message = LocalMessage.current() // returns "hello"
 *
 *   PinnedContext.current()
 * }.get()
 *
 * executor.submit(pin) {
 *   val message = LocalMessage.current() // returns "hello"
 *   println(message)
 * }
 * ```
 */
public sealed interface ContextLocal<out T> {
  /** Returns the value set for the current active context, or `null` if the value has not been set. */
  @ContextAware public fun current(): T?
}

/** Mutable context local implementation used by [ContextAwareExecutor]. */
@InternalExecutorApi internal class ContextLocalImpl<T> : ContextLocal<T> {
  private val currentMap: MutableMap<Any, Any?>
    get() = localValues.get() ?: throw NoActiveContextError("ContextLocal")

  override fun current(): T? {
    @Suppress("UNCHECKED_CAST")
    return currentMap[this] as T?
  }

  fun set(value: T) = currentMap.put(this, value)

  @Suppress("UNCHECKED_CAST")
  fun clear(): T? = currentMap.remove(this) as T?

  companion object {
    private val localValues = ThreadLocal<UnsafeContextLocalMap>()

    fun attach(values: UnsafeContextLocalMap) = localValues.set(values)
    fun detach() = localValues.remove()
  }
}

/** Creates a new empty [ContextLocal] value. */
@OptIn(InternalExecutorApi::class)
public fun <T> ContextLocal(): ContextLocal<T> = ContextLocalImpl()
