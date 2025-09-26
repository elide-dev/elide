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

import java.lang.AutoCloseable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * # Action Scope
 */
public sealed interface ActionScope : CoroutineScope, AutoCloseable {
  public val actionContext: Action.ActionContext
  public val taskScope: TaskGraphScope
  public val allTasks: Sequence<StructuredTaskScope.Subtask<*>>
  public val logging: Logger
  public fun bind(execution: TaskGraphExecution.Listener)
  public fun currentExecution(): TaskGraphExecution.Listener
  public fun <R> register(subtask: StructuredTaskScope.Subtask<R>): StructuredTaskScope.Subtask<R>

  public class DefaultActionScope internal constructor (
    override val actionContext: Action.ActionContext,
    override val coroutineContext: CoroutineContext,
    override val taskScope: TaskGraphScope,
    override val logging: Logger,
  ) : ActionScope {
    // Bound execution scope, if any.
    private val boundScope = atomic<TaskGraphExecution.Listener?>(null)

    // Registered subtasks.
    private val allRegisteredSubtasks = ConcurrentLinkedQueue<StructuredTaskScope.Subtask<*>>()

    override fun <R> register(subtask: StructuredTaskScope.Subtask<R>): StructuredTaskScope.Subtask<R> {
      return subtask.also { allRegisteredSubtasks.add(it) }
    }

    // Retrieve all tasks seen by this scope.
    override val allTasks: Sequence<StructuredTaskScope.Subtask<*>> get() = allRegisteredSubtasks.asSequence()

    override fun bind(execution: TaskGraphExecution.Listener) {
      boundScope.value = execution
    }

    override fun currentExecution(): TaskGraphExecution.Listener = requireNotNull(boundScope.value) {
      "No bound execution scope"
    }

    override fun close() {
      super.close()
    }
  }

  override fun close() {
    // no-op
  }

  public class TaskGraphScope internal constructor (
    internal val name: String,
    internal val fac: ThreadFactory,
  ) {
    internal val scope = StructuredTaskScope.open<Any?>()
  }

  /** Factories for creating or obtaining an [ActionScope]. */
  public companion object {
    // Create a default thread factory for execution of a task graph.
    private fun namedStructuredScope(name: String): TaskGraphScope = TaskGraphScope(
      name,
      Thread.ofVirtual().name("$name-exec-", 1L).factory()
    )

    // Create a default thread factory for execution of a task graph.
    private fun defaultStructuredTaskScope(): TaskGraphScope = namedStructuredScope(
      "elide-default-graph"
    )

    /** @return Named action scope, configured with the provided parameters. */
    @JvmStatic public fun named(name: String, actions: Action.ActionContext): ActionScope = create(
      actions,
      Dispatchers.Default,
      namedStructuredScope(name),
    )

    /** @return Default action scope, configured with the provided parameters. */
    @JvmStatic public fun create(actions: Action.ActionContext): ActionScope = create(actions, Dispatchers.Default)

    /** @return Default action scope, configured with the provided parameters. */
    @JvmStatic public fun create(actions: Action.ActionContext, coroutines: CoroutineContext): ActionScope {
      return create(actions, coroutines, defaultStructuredTaskScope())
    }

    /** @return Default action scope, configured with the provided parameters. */
    @JvmStatic public fun named(ctx: Action.ActionContext, coroutines: CoroutineContext, name: String): ActionScope {
      return create(ctx, coroutines, namedStructuredScope(name))
    }

    /** @return Default action scope, configured with the provided parameters. */
    @JvmStatic public fun create(
      actions: Action.ActionContext,
      coroutines: CoroutineContext,
      scope: TaskGraphScope,
    ): ActionScope {
      return DefaultActionScope(actions, coroutines, scope, Logging.of(ActionScope::class))
    }
  }
}
