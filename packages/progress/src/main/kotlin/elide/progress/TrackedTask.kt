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
package elide.progress

/**
 * Immutable state of a task in a progress animation.
 *
 * @property name Name of the task.
 * @property status Current status of the task.
 * @property output Unix timestamps mapped to lines of console output of the task.
 * @property position Current position of the task, or `-1` if the task has not started.
 * @property target Target position of the task. If this is `1`, the task is rendered as indeterminate.
 * @property started `true` if the task has started ([position] is not negative).
 * @property finished `true` if the task has finished ([position] is equal to [target]).
 * @property state current state of this task.
 * @author Lauri Heino <datafox>
 */
public data class TrackedTask(
  val name: String,
  val target: Int,
  val status: String = "",
  val position: Int = -1,
  val output: Map<Long, String> = mapOf(),
  val failed: Boolean = false,
) {
  val started: Boolean get() = position >= 0
  val finished: Boolean get() = position == target
  val state: TaskState get() = when {
    failed -> TaskState.FAILED
    !started -> TaskState.NOT_STARTED
    !finished -> TaskState.RUNNING
    else -> TaskState.COMPLETED
  }
}
