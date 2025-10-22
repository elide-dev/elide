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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of [ProgressManager].
 *
 * @author Lauri Heino <datafox>
 */
internal class ProgressManagerImpl(name: String, terminal: Terminal) : ProgressManager {
  override val progress: Progress = ProgressImpl(name, terminal, emptyList())
  private val tasks: MutableMap<String, TaskData> = mutableMapOf()
  private val taskLock: Mutex = Mutex()

  override suspend fun register(
    id: String,
    name: String,
    target: Int,
    status: String,
    scope: CoroutineScope,
    events: Flow<TaskEvent>,
  ): StateFlow<TrackedTask> {
    if (target <= 0) throw IllegalArgumentException("Target must be a non-zero positive integer")
    taskLock.withLock {
      if (id in tasks) throw IllegalArgumentException("Task with id \"$id\" is already registered.")
      TaskData(progress.addTask(name, target, status)).apply {
        tasks.put(id, this)
        job = scope.launchJob(this, events)
      }
    }
    if (!progress.running) progress.start()
    return track(id)!!
  }

  override suspend fun track(id: String): StateFlow<TrackedTask>? =
    taskLock.withLock { tasks[id] }?.let { progress.getTaskFlow(it.index) }

  override suspend fun stop(id: String) {
    taskLock.withLock {
      tasks[id]?.apply {
        job.cancel()
        stopTask(this)
      }
    }
  }

  override suspend fun stopAll() {
    taskLock.withLock {
      tasks.values.forEach {
        it.job.cancel()
        it.running = false
      }
    }
    progress.stop()
  }

  private fun CoroutineScope.launchJob(data: TaskData, events: Flow<TaskEvent>): Job = launch {
    events.catch {
      if (it is CancellationException) throw it
      progress.updateTask(data.index) { copy(failed = true) }
    }.onCompletion { throwable ->
      if (throwable == null) progress.updateTask(data.index) { copy(position = target) }
      taskLock.withLock { stopTask(data) }
    }.collect { collectEvent(data, it) }
  }

  private suspend fun stopTask(data: TaskData) {
    data.running = false
    if (progress.running && tasks.values.none { it.running }) progress.stop()
  }

  private suspend fun collectEvent(data: TaskData, task: TaskEvent) {
    when (task) {
      is StatusMessage -> progress.updateTask(data.index) { copy(status = task.status) }
      is ProgressPosition -> updatePosition(data.index, task.position)
      is AppendOutput -> {
        val time = Clock.System.now().toEpochMilliseconds()
        progress.updateTask(data.index) { copy(output = this.output + (time to task.output)) }
      }
      is TaskStarted -> if(task.started) updatePosition(data.index, 0)
      is TaskCompleted -> if (task.completed) updatePosition(data.index, Int.MAX_VALUE)
      is TaskFailed -> if (task.failed) progress.updateTask(data.index) { copy(failed = true) }
    }
  }

  private suspend fun updatePosition(index: Int, position: Int) {
    progress.updateTask(index) {
      Math.clamp(position.toLong(), this.position, target).let { if (it > this.position) copy(position = it) else this }
    }
  }

  private data class TaskData(val index: Int, var running: Boolean = true) {
    lateinit var job: Job
  }
}
