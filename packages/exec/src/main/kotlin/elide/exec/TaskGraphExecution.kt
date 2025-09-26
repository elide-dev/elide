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
package elide.exec

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.StructuredTaskScope
import kotlinx.coroutines.Deferred

/**
 * Responder function for events emitted by the graph.
 */
public typealias TaskGraphResponder = suspend EventResponderContext<Any, out Event>.() -> Unit

/**
 * ID of a listener so it can later be un-listened.
 */
public typealias ListenerId = Int

/**
 * Binder function for execution events; executed before the graph begins running.
 */
public typealias ExecutionBinder = (TaskGraphExecution.Listener.() -> Unit)

/**
 * Describes a registered listener.
 */
public typealias ResponderWithId = Pair<ListenerId, TaskGraphResponder>

/**
 * Context in which an event responder function is dispatched.
 */
public interface EventResponderContext<T, E : Event> {
  /**
   * Event under consideration.
   */
  public val event: E

  /**
   * Extra context for this event.
   */
  public val context: T
}

/**
 * # Task Graph Execution
 */
public interface TaskGraphExecution {
  /**
   * Root coroutines job holding this execution.
   */
  public val rootJob: Deferred<Unit>

  /**
   * Await completion of this execution.
   */
  public suspend fun await(): Listener

  /**
   * @return All tasks.
   */
  public fun tasks(): List<Task>

  /**
   * Affix an event listener for the provided [event].
   *
   * @param event Event to listen for
   * @param listener Listener to invoke when the event is emitted
   */
  public fun <E : Event> bind(event: E, listener: TaskGraphResponder): ListenerId

  /**
   * Stop delivering events for the provided listener [id].
   *
   * @param id ID of the listener to remove
   */
  public fun remove(id: ListenerId)

  /**
   * ## Execution Listener
   */
  @Suppress("unused")
  public class Listener internal constructor (
    private val graph: TaskGraph,
    private val scope: ActionScope,
    private val taskScope: ActionScope.TaskGraphScope,
    private val latch: CountDownLatch,
  ) : TaskGraphExecution {
    override lateinit var rootJob: Deferred<Unit>

    // Listeners for each event type.
    private val listenerMap: MutableMap<Event, MutableList<ResponderWithId>> = ConcurrentSkipListMap()

    override fun tasks(): List<Task> {
      require(graph.isComplete()) { "Cannot poll for all tasks until graph is complete" }
      return graph.poll().toList()
    }

    override fun <E : Event> bind(event: E, listener: TaskGraphResponder): ListenerId {
      val listeners = listenerMap.getOrDefault(event, ArrayList<ResponderWithId>())
      val id = System.identityHashCode(listener)
      val entry: ResponderWithId = id to listener
      listeners.add(entry)
      listenerMap[event] = listeners
      return id
    }

    override fun remove(id: ListenerId) {
      listenerMap.forEach { (_, listeners) ->
        listeners.removeIf { it.first == id }
      }
    }

    // Dispatch `event` to the current listeners with provided `context`.
    internal suspend fun <E : Event> dispatch(event: E, context: Any) {
      // nothing at this time
      val ctx = object : EventResponderContext<Any, E> {
        override val context: Any get() = context
        override val event: E get() = event
      }
      listenerMap[event]?.forEach {
        it.second.invoke(ctx)
      }
    }

    override suspend fun await(): Listener = apply {
      rootJob.await()
      latch.await()
    }
  }
}

@Suppress("UNCHECKED_CAST")
public inline fun <reified E : Event> TaskGraphExecution.Listener.on(
  event: E,
  crossinline responder: suspend EventResponderContext<Any, E>.() -> Unit,
) {
  bind(event) {
    (this as EventResponderContext<Any, E>)
    responder.invoke(this)
  }
}
