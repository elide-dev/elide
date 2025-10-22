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
package elide.progress.impl

import com.github.ajalt.mordant.terminal.Terminal
import elide.progress.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion

/**
 * Implementation of [ProgressManager].
 *
 * @author Lauri Heino <datafox>
 */
internal class ProgressManagerImpl(name: String, terminal: Terminal) : ProgressManager {
  override val progress: Progress = ProgressImpl(name, terminal, emptyList())
  private val tasks: MutableMap<String, TaskData> = ConcurrentHashMap()
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  override suspend fun register(
    id: String,
    name: String,
    target: Int,
    status: String,
    events: Flow<TaskEvent>,
  ): StateFlow<TrackedTask> {
    if (target <= 0) throw IllegalArgumentException("Target must be a non-zero positive integer")
    if (id in tasks) throw IllegalArgumentException("Task with id \"$id\" is already registered.")
    tasks.put(id, launchJob(id, progress.addTask(name, target, status), events))
    if (!progress.running) progress.start()
    return track(id)!!
  }

  override suspend fun track(id: String): StateFlow<TrackedTask>? = tasks[id]?.let { progress.getTaskFlow(it.index) }

  override suspend fun stop(id: String) {
    tasks[id]?.apply {
      job.cancel()
      stopTask(this)
    }
  }

  override suspend fun stopAll() {
    tasks.values.forEach {
      it.job.cancel()
      it.running = false
    }
    progress.stop()
  }

  private fun launchJob(id: String, index: Int, events: Flow<TaskEvent>): TaskData {
    val job = scope.launch {
      events.catch {
        if (it is CancellationException) throw it
        progress.updateTask(index) { copy(failed = true) }
      }.onCompletion { throwable ->
        if (throwable == null) progress.updateTask(index) { copy(position = target) }
        stopTask(id)
      }.collect { collectEvent(index, it) }
    }
    return TaskData(index, job)
  }

  private suspend fun stopTask(data: TaskData) {
    data.running = false
    if (progress.running && tasks.values.none { it.running }) progress.stop()
  }

  private suspend fun stopTask(id: String) {
    tasks[id]?.let { stopTask(it) }
  }

  private suspend fun collectEvent(index: Int, task: TaskEvent) {
    when (task) {
      is StatusMessage -> progress.updateTask(index) { copy(status = task.status) }
      is ProgressPosition -> updatePosition(index, task.position)
      is AppendOutput -> {
        val time = Clock.System.now().toEpochMilliseconds()
        progress.updateTask(index) { copy(output = this.output + (time to task.output)) }
      }
      is TaskStarted -> if (task.started) updatePosition(index, 0)
      is TaskCompleted -> if (task.completed) updatePosition(index, Int.MAX_VALUE)
      is TaskFailed -> if (task.failed) progress.updateTask(index) { copy(failed = true) }
    }
  }

  private suspend fun updatePosition(index: Int, position: Int) {
    progress.updateTask(index) {
      Math.clamp(position.toLong(), this.position, target).let { if (it > this.position) copy(position = it) else this }
    }
  }

  private data class TaskData(val index: Int, val job: Job, var running: Boolean = true)
}
