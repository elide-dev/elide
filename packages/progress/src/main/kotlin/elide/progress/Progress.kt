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

import com.github.ajalt.mordant.terminal.Terminal
import elide.progress.impl.ProgressImpl
import elide.progress.impl.ProgressManagerImpl

/**
 * A low-level interface for rendering a progress animation to the console.
 *
 * @property name Name of the main process.
 * @property tasks Current tasks of the progress animation.
 * @author Lauri Heino <datafox>
 */
public interface Progress {
  public val name: String
  public val tasks: List<TrackedTask>

  /** Starts rendering the animation. */
  public suspend fun start()

  /** Stops rendering the animation. */
  public suspend fun stop()

  /** Returns the task with the specified index. */
  public suspend fun getTask(index: Int): TrackedTask

  /** Adds a new task and returns its index. */
  public suspend fun addTask(name: String, target: Int, status: String = ""): Int

  /** Updates a task with the specified index. */
  public suspend fun updateTask(index: Int, block: TrackedTask.() -> TrackedTask)

  public companion object {
    /** Creates a new progress animation that renders to [terminal]. */
    public fun create(name: String, terminal: Terminal, tasks: MutableList<TrackedTask>.() -> Unit): Progress =
      ProgressImpl(name, terminal, mutableListOf<TrackedTask>().apply(tasks))

    /**
     * Creates a new [ProgressManager] for higher level management of a progress animation that renders to [terminal].
     */
    public fun managed(name: String, terminal: Terminal): ProgressManager = ProgressManagerImpl(name, terminal)
  }
}
