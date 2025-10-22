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
package elide.tool.cli.progress

import kotlinx.coroutines.flow.FlowCollector

/**
 * An event used by [ProgressManager] to update the state of a task in a progress animation.
 *
 * @author Lauri Heino <datafox>
 */
sealed interface TaskEvent

/** Value class for an event that updates a task's status message. */
@JvmInline value class StatusMessage(val status: String) : TaskEvent

/** Value class for an event that updates a task's progress bar. */
@JvmInline value class ProgressPosition(val position: Int) : TaskEvent

/** Value class for an event that appends to a task's console. */
@JvmInline value class AppendOutput(val output: String) : TaskEvent

/** Value class for an event that starts a task (sets position to `0` if it is `-1`). */
@JvmInline value class TaskStarted(val started: Boolean = true) : TaskEvent

/** Value class for an event that fails a task. */
@JvmInline value class TaskFailed(val failed: Boolean = true) : TaskEvent

/** Value class for an event that completes a task (sets position to target). */
@JvmInline value class TaskCompleted(val completed: Boolean = true) : TaskEvent

/** Updates a task's status message. */
suspend fun FlowCollector<TaskEvent>.emitStatus(status: String): Unit = emit(StatusMessage(status))

/** Updates a task's progress bar. */
suspend fun FlowCollector<TaskEvent>.emitProgress(position: Int): Unit = emit(ProgressPosition(position))

/** Appends to a task's console. */
suspend fun FlowCollector<TaskEvent>.emitOutput(output: String): Unit = emit(AppendOutput(output))

/** Starts a task (sets position to `0` if it is `-1`). */
suspend fun FlowCollector<TaskEvent>.emitStarted(): Unit = emit(TaskStarted())

/** Fails a task. */
suspend fun FlowCollector<TaskEvent>.emitFailed(): Unit = emit(TaskFailed())

/** Completes a task (sets position to target). */
suspend fun FlowCollector<TaskEvent>.emitCompleted(): Unit = emit(TaskCompleted())
