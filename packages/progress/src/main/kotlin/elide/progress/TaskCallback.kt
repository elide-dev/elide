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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/**
 * A high-level interface for managing a single task of a progress animation.
 *
 * @property progress [Progress] instance managed by this callback.
 * @property index Index of the task managed by this callback.
 * @property task State of the task managed by this callback.
 * @author Lauri Heino <datafox>
 */
public interface TaskCallback {
  public val progress: Progress
  public val index: Int
  public val task: TrackedTask

  /** Sets the task status message to the return value of [status]. */
  public suspend fun setStatus(status: TrackedTask.() -> String)

  /** Sets the task progress position to the return value of [progress]. */
  public suspend fun setPosition(position: TrackedTask.() -> Int)

  /** Appends the return value of [output] to the task's console output. */
  public suspend fun appendOutput(output: TrackedTask.() -> String)

  /**
   * Fails the task. If [finish] is `true`, also sets the task's progress position to its target, marking the task as
   * finished.
   */
  public suspend fun fail(finish: Boolean = true)

  /** Subscribes to [events] within [scope] and returns the collecting [Job]. */
  public suspend fun subscribe(events: Flow<TaskEvent>, scope: CoroutineScope): Job

  /** Cancels the [Job] created when [subscribe] is called. Does nothing if [subscribe] has not been called. */
  public suspend fun stop()

  /** Sets the task status message to [status]. */
  public suspend fun setStatus(status: String): Unit = setStatus { status }

  /** Sets the task progress position to [progress]. */
  public suspend fun setPosition(position: Int): Unit = setPosition { position }

  /** Appends [output] to the task's console output. */
  public suspend fun appendOutput(output: String): Unit = appendOutput { output }
}
