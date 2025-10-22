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

import kotlin.experimental.ExperimentalTypeInference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * A high-level interface for managing a [Progress] animation.
 *
 * @property progress [Progress] instance managed by this progress manager.
 * @author Lauri Heino <datafox>
 */
public interface ProgressManager {
  public val progress: Progress

  /**
   * Adds a new task with [id] to the [progress] that listens to [events] and returns a [StateFlow] for the state of
   * that task. If [target] is `1`, the task is rendered as indeterminate.
   */
  public suspend fun register(
    id: String,
    name: String,
    target: Int = 1,
    status: String = "",
    events: Flow<TaskEvent>,
  ): StateFlow<TrackedTask>

  /** Returns a [StateFlow] for the state of the task with [id], or `null` if no task is registered. */
  public suspend fun track(id: String): StateFlow<TrackedTask>?

  /** Stops the task with [id] and stops rendering the animation if no tasks are running. */
  public suspend fun stop(id: String)

  /** Stops all tasks and rendering the animation. */
  public suspend fun stopAll()
}

/**
 * Adds a new task with [id] to the [Progress] that listens to [flow] { [block] } and returns a [StateFlow] for the
 * state of that task. If [target] is `1`, the task is rendered as indeterminate.
 */
@OptIn(ExperimentalTypeInference::class)
public suspend fun ProgressManager.register(
  id: String,
  name: String,
  target: Int = 1,
  status: String = "",
  @BuilderInference block: suspend FlowCollector<TaskEvent>.() -> Unit
): StateFlow<TrackedTask> = register(id, name, target, status, flow(block))
