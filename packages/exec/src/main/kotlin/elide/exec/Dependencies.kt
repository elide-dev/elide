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

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * # Dependencies
 */
@Serializable
public sealed interface Dependencies : Satisfiable {
  /**
   *
   */
  public data object None : Dependencies {
    private val emptyJob by lazy { Job() }
    override val status: Status get() = Status.READY
    override val job: Job get() = emptyJob
  }

  /**
   *
   */
  public data object Implied : Dependencies {
    private val emptyJob by lazy { Job() }
    override val status: Status get() = Status.NONE
    override val job: Job get() = emptyJob
  }

  /**
   *
   */
  public data class TaskDependencies(public val scope: ActionScope, public val tasks: List<Task>) : Dependencies {
    override val job: Job get() = scope.launch {
      tasks.map { it.job }.joinAll()
    }

    override val status: Status get() = when {
      tasks.isEmpty() -> Status.READY
      else -> tasks.let {
        var terminalStatus: Status? = null
        var defaultStatus = Status.READY
        var allHaveFinished = true
        var allHaveSucceeded = true
        for (task in it) {
          when (task.status) {
            Status.PENDING, Status.QUEUED -> {
              allHaveFinished = false
              defaultStatus = Status.PENDING
            }
            Status.RUNNING -> {
              allHaveFinished = false
              defaultStatus = Status.RUNNING
            }
            Status.FAIL -> {
              allHaveSucceeded = false
              terminalStatus = Status.FAIL
            }
            Status.READY,
            Status.SUCCESS,
            Status.NONE -> { /* do nothing */ }
          }
        }
        terminalStatus ?: when {
          allHaveFinished && allHaveSucceeded -> Status.SUCCESS
          allHaveFinished && !allHaveSucceeded -> Status.FAIL
          else -> defaultStatus
        }
      }
    }
  }
}

/**
 * Build a suite of task-based dependencies; this will also enforce task completion before the action is executed.
 *
 * @receiver ActionScope to use for this action.
 * @param tasks Tasks to use as dependencies for this action.
 * @return A [Dependencies.TaskDependencies] instance that will enforce task completion before the action is executed.
 */
public fun ActionScope.taskDependencies(tasks: List<Task>): Dependencies {
  return if (tasks.isEmpty()) {
    Dependencies.None
  } else Dependencies.TaskDependencies(
    this,
    tasks,
  )
}
