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
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
  private val _tasks: MutableList<MutableStateFlow<TrackedTask>> =
    CopyOnWriteArrayList(tasks.map { MutableStateFlow(it) })
  private var animation: MutableStateFlow<Animation<ProgressState>?> = MutableStateFlow(null)
  override val tasks: List<TrackedTask> get() = _tasks.map { it.value }

  override val running: Boolean get() = animation.value != null

  override suspend fun start() {
    if (animation.value != null) throw IllegalStateException("Already started")
    animation.value = terminal.animation { ProgressRenderer.render(it) }
    updateAnimation()
  }

  override suspend fun stop() {
    animation.value?.stop() ?: throw IllegalStateException("Already stopped")
    animation.value = null
  }

  override suspend fun getTask(index: Int): TrackedTask = _tasks[index].value

  override suspend fun getTaskFlow(index: Int): StateFlow<TrackedTask> = _tasks[index].asStateFlow()

  override suspend fun addTask(name: String, target: Int, status: String): Int {
    val stateFlow = MutableStateFlow(TrackedTask(name, target, status))
    _tasks.add(stateFlow)
    updateAnimation()
    return _tasks.indexOf(stateFlow)
  }

  override suspend fun updateTask(index: Int, block: TrackedTask.() -> TrackedTask) {
    _tasks[index].update(block)
    updateAnimation()
  }

  private fun updateAnimation() {
    animation.value?.update(ProgressState(name, tasks))
  }
}
