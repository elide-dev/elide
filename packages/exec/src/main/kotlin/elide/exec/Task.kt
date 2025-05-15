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

import com.google.errorprone.annotations.Immutable
import com.google.errorprone.annotations.ThreadSafe
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.exec.Action.ActionContext
import elide.exec.TaskGraphEvent.*
import elide.runtime.Logging

// Whether to emit debug logs for task execution.
private const val TASK_DEBUG_LOG = false

// Whether to force task execution to fail on-error.
private const val TASK_DEBUG_FORCE_CRASH_ON_ERROR = false

// Whether to force task execution to fail always.
private const val TASK_DEBUG_FORCE_CRASH_ALWAYS = false

/**
 * # Task
 */
@Serializable
public sealed interface Task : Satisfiable {
  /**
   * ID uniquely identifying this task.
   */
  public val id: TaskId

  /**
   * Current status of this task.
   */
  override val status: Status

  /**
   * Action associated with this task.
   */
  public val action: Action

  /**
   * Dependencies for this task.
   */
  public val dependencies: Dependencies

  /**
   * Inputs associated with this task.
   */
  public val inputs: Inputs

  /**
   * Outputs associated with this task.
   */
  public val outputs: Outputs

  /**
   * Transition this task to the provided status immediately.
   *
   * @param to Status to transition to.
   */
  public fun transition(to: Status)

  /**
   * Describe this task in a human-readable way; this is used in terminal UI.
   *
   * @return Description string.
   */
  public fun describe(): String

  /**
   * Execute the action which this task is attached to, managing status transitions as we go.
   *
   * @param scope Scope to execute this task in
   * @return Job which can be joined
   */
  public suspend fun executeTask(scope: ActionScope): Job

  /**
   * ## Task Status
   *
   * Holds status in potentially mutable form for a task.
   */
  @Serializable
  public class TaskStatus internal constructor (internal var status: Status) : Comparable<TaskStatus> {
    override fun toString(): String = status.toString()
    override fun hashCode(): Int = status.hashCode()
    override fun compareTo(other: TaskStatus): Int = status.compareTo(other.status)
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is TaskStatus) return false
      return status == other.status
    }
  }

  /**
   * ## Default Task
   */
  @JvmRecord @Serializable @Immutable @ThreadSafe public data class DefaultTask internal constructor (
    override val id: TaskId,
    override val action: Action,
    override val job: Job,
    override val dependencies: Dependencies = Dependencies.None,
    override val inputs: Inputs = Inputs.None,
    override val outputs: Outputs = Outputs.None,
    internal val taskStatus: TaskStatus = TaskStatus(Status.QUEUED),
    @Transient private val err: AtomicReference<Throwable?> = AtomicReference(null),
    @Transient private val result: AtomicReference<Result?> = AtomicReference(null),
    @Transient private val description: AtomicReference<(Task.() -> String)?> = AtomicReference(null),
  ) : Task {
    public fun describedBy(description: String): DefaultTask = apply {
      this.description.set({ description })
    }

    public fun describedBy(descriptionProvider: Task.() -> String): DefaultTask = apply {
      this.description.set(descriptionProvider)
    }

    private fun ActionScope.debugLog(msg: () -> String) {
      if (TASK_DEBUG_LOG) {
        logging.trace { "[build:graph] ${msg()}" }
      }
    }

    override fun describe(): String = description.get()?.invoke(this) ?: error("No description for task: $this")
    override val status: Status get() = taskStatus.status

    override fun transition(to: Status) {
      taskStatus.status = to
    }

    override suspend fun executeTask(scope: ActionScope): Job = scope.currentExecution().let { execution ->
      scope.debugLog { "Launching job for task '$id'" }

      scope.launch {
        scope.debugLog { "Emitting 'TaskExecute' for '$id'" }
        execution.dispatch(TaskExecute, this@DefaultTask)
        var didSucceed = false
        var taskErr: Throwable? = null

        val doSuccess = suspend {
          scope.debugLog { "Task '$id' finished without error; emitting 'TaskCompleted'" }
          execution.dispatch(TaskCompleted, this@DefaultTask)
          scope.debugLog { "Emitting 'TaskFinished' for '$id'" }
          execution.dispatch(TaskFinished, this@DefaultTask)
          transition(Status.SUCCESS)
        }
        val doFailure = suspend {
          scope.debugLog { "Task '$id' finished exceptionally: ${taskErr ?: "<no error>"}" }
          err.set(taskErr)
          scope.debugLog { "Emitting 'TaskFailed' for '$id'" }
          execution.dispatch(TaskFailed, this@DefaultTask)
          scope.debugLog { "Emitting 'TaskFinished' for '$id'" }
          execution.dispatch(TaskFinished, this@DefaultTask)
          transition(Status.FAIL)
        }

        runCatching {
          didSucceed = completeWith(action(scope))
        }.onSuccess {
          when (didSucceed) {
            true -> doSuccess()
            false -> doFailure()
          }
        }.onFailure {
          didSucceed = false
          taskErr = it
          doFailure()
          if (TASK_DEBUG_FORCE_CRASH_ON_ERROR) {
            // force failure if requested.
            it.printStackTrace()
            error("Task failed: $this")
          }
        }
      }.also {
        scope.debugLog { "Job launched for task at ID '$id'" }
      }
    }

    private fun completeWith(result: Result): Boolean {
      return if (TASK_DEBUG_FORCE_CRASH_ALWAYS) {
        Logging.root().error("Task '$id' force-failed")
        false
      } else {
        this.result.set(result)
        result.isSuccess
      }
    }
  }

  /** Factories for obtaining [Task] instances. */
  public companion object {
    /**
     * Wrap an arbitrary suspending function as an [action], using the provided inputs, or calculating sensible defaults
     * for each; in the simplest case, only the [action] is provided.
     */
    @JvmStatic public fun ActionScope.fn(
      name: String? = null,
      dependencies: Dependencies = Dependencies.None,
      inputs: Inputs = Inputs.None,
      outputs: Outputs = Outputs.None,
      status: Status = Status.QUEUED,
      id: TaskId? = null,
      action: suspend ActionContext.() -> Result,
    ): DefaultTask = async(this.coroutineContext, start = CoroutineStart.LAZY) {
      actionContext.action()
    }.let { job ->
      DefaultTask(
        id = id ?: if (name != null) TaskId.fromName(name) else TaskId.defaultFrom(action),
        job = job,
        taskStatus = TaskStatus(status),
        dependencies = dependencies,
        inputs = inputs,
        outputs = outputs,
        action = Action.of(job),
      )
    }
  }
}
