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

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import elide.tool.cli.progress.impl.ProgressImpl
import elide.tool.cli.progress.impl.ProgressManagerImpl

/**
 * A low-level interface for rendering a progress animation to the console.
 *
 * @property name Name of the main process.
 * @property tasks Current tasks of the progress animation.
 * @property running `true` if the progress animation is being rendered.
 * @author Lauri Heino <datafox>
 */
interface Progress {
  val name: String
  val tasks: List<TrackedTask>
  val running: Boolean

  /** Starts rendering the animation. */
  suspend fun start()

  /** Stops rendering the animation. */
  suspend fun stop()

  /** Returns the state of a task at [index]. */
  suspend fun getTask(index: Int): TrackedTask

  /** Returns the [StateFlow] of a task at [index]. */
  suspend fun getTaskFlow(index: Int): StateFlow<TrackedTask>

  /** Adds a new task and returns its index. If [target] is `1`, the task is rendered as indeterminate. */
  suspend fun addTask(name: String, target: Int = 1, status: String = ""): Int

  /** Updates the state of a task at [index]. */
  suspend fun updateTask(index: Int, block: TrackedTask.() -> TrackedTask)

  companion object {
    /** Creates a new progress animation that renders to [terminal]. */
    fun create(name: String, terminal: Terminal, tasks: MutableList<TrackedTask>.() -> Unit): Progress =
      ProgressImpl(name, terminal, mutableListOf<TrackedTask>().apply(tasks))

    /**
     * Creates a new [ProgressManager] for higher level management of a progress animation that renders to [terminal].
     */
    fun managed(name: String, terminal: Terminal, context: CoroutineContext? = null): ProgressManager =
      ProgressManagerImpl(name, terminal, context)
  }
}
