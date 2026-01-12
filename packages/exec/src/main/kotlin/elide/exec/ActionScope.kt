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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * # Action Scope
 */
public sealed interface ActionScope : CoroutineScope, AutoCloseable {
  public val actionContext: Action.ActionContext
  public val taskScope: TaskGraphScope
  public val allTasks: Sequence<Job>
  public val logging: Logger
  public fun bind(execution: TaskGraphExecution.Listener)
  public fun currentExecution(): TaskGraphExecution.Listener
  public fun register(job: Job): Job

  public class DefaultActionScope internal constructor (
    override val actionContext: Action.ActionContext,
    override val coroutineContext: CoroutineContext,
    override val taskScope: TaskGraphScope,
    override val logging: Logger,
  ) : ActionScope {
    // Bound execution scope, if any.
    private val boundScope = atomic<TaskGraphExecution.Listener?>(null)

    // Registered jobs.
    private val allRegisteredJobs = ConcurrentLinkedQueue<Job>()

    override fun register(job: Job): Job {
      return job.also { allRegisteredJobs.add(it) }
    }

    // Retrieve all tasks seen by this scope.
    override val allTasks: Sequence<Job> get() = allRegisteredJobs.asSequence()

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
  ) {
    // Track active jobs for this task graph
    internal val jobs = ConcurrentLinkedQueue<Job>()
  }

  /** Factories for creating or obtaining an [ActionScope]. */
  public companion object {
    // Create a named task graph scope.
    private fun namedStructuredScope(name: String): TaskGraphScope = TaskGraphScope(name)

    // Create a default task graph scope.
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
