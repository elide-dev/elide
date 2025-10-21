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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import elide.progress.Progress
import elide.progress.TaskCallback
import elide.progress.TaskEvent
import elide.progress.TrackedTask

/**
 * Implementation of [TaskCallback].
 *
 * @author Lauri Heino <datafox>
 */
internal class TaskCallbackImpl(override val progress: Progress, override val index: Int) : TaskCallback {
  override val task: TrackedTask get() = runBlocking { progress.getTask(index) }
  private lateinit var collectJob: Job
  private val collectLock: Mutex = Mutex()

  override suspend fun setStatus(status: TrackedTask.() -> String) =
    progress.updateTask(index) { copy(status = status()) }

  override suspend fun setPosition(position: TrackedTask.() -> Int) =
    progress.updateTask(index) {
      Math.clamp(position().toLong(), this.position, target).let {
        if (it > this.position) copy(position = it) else this
      }
    }

  override suspend fun appendOutput(output: TrackedTask.() -> String) {
    val time = Clock.System.now().toEpochMilliseconds()
    progress.updateTask(index) { copy(output = this.output + (time to output())) }
  }

  override suspend fun fail(finish: Boolean) =
    progress.updateTask(index) { copy(position = if (finish) target else position, failed = true) }

  override suspend fun subscribe(events: Flow<TaskEvent>, scope: CoroutineScope): Job =
    collectLock.withLock {
      if (::collectJob.isInitialized) throw IllegalStateException("Already subscribed")
      scope.launch {
        events.catch {
          if (it is CancellationException) throw it
          fail()
        }.collect { event ->
          event.status?.let { setStatus(it) }
          event.position?.let { setPosition(it) }
          event.message?.let { appendOutput(it) }
          if (event.failed) fail(false)
        }
      }.apply { collectJob = this }
    }

  override suspend fun stop() = collectLock.withLock { if (::collectJob.isInitialized) collectJob.cancel() }
}
