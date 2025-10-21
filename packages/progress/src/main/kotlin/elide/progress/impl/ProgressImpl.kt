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

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import elide.progress.Progress
import elide.progress.ProgressRenderer
import elide.progress.ProgressState
import elide.progress.TrackedTask

/**
 * Implementation of [Progress].
 *
 * @author Lauri Heino <datafox>
 */
internal class ProgressImpl(
  override val name: String,
  private val terminal: Terminal,
  tasks: List<TrackedTask> = emptyList()
) : Progress {
  private val _tasks: MutableList<MutableStateFlow<TrackedTask>> = tasks.map { MutableStateFlow(it) }.toMutableList()
  private var animation: Animation<ProgressState>? = null
  private val taskLock: Mutex = Mutex()
  private val animationLock: Mutex = Mutex()
  override val tasks: List<TrackedTask>
    get() = runBlocking { taskLock.withLock { _tasks.map { it.value } } }

  override suspend fun start() {
    animationLock.withLock {
      if (animation != null) throw IllegalStateException("Already started")
      animation = terminal.animation { ProgressRenderer.render(it) }
    }
    updateAnimation()
  }

  override suspend fun stop() =
    animationLock.withLock { animation?.stop() ?: throw IllegalStateException("Not started") }

  override suspend fun getTask(index: Int): TrackedTask = taskLock.withLock { _tasks[index] }.value

  override suspend fun addTask(name: String, target: Int, status: String): Int {
    val task = TrackedTask(name, target, status)
    val index = taskLock.withLock { _tasks.size.apply { _tasks.add(MutableStateFlow(task)) } }
    updateAnimation()
    return index
  }

  override suspend fun updateTask(index: Int, block: TrackedTask.() -> TrackedTask) {
    taskLock.withLock { _tasks[index] }.update(block)
    updateAnimation()
  }

  private suspend fun updateAnimation() {
    animationLock.withLock { animation }?.update(ProgressState(name, taskLock.withLock { _tasks.map { it.value } }))
  }
}
