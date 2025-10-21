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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import elide.progress.Progress
import elide.progress.ProgressManager
import elide.progress.TaskCallback

/**
 * Implementation of [ProgressManager].
 *
 * @author Lauri Heino <datafox>
 */
internal class ProgressManagerImpl(name: String, terminal: Terminal) : ProgressManager {
  override val progress: Progress = ProgressImpl(name, terminal, emptyList())
  private val callbacks: MutableList<TaskCallbackImpl> = mutableListOf()
  private val callbackLock: Mutex = Mutex()

  override suspend fun addTask(name: String, target: Int, status: String): TaskCallback {
    if (target <= 0) throw IllegalArgumentException("Target must be a non-zero positive integer")
    val task = progress.addTask(name, target, status)
    return TaskCallbackImpl(progress, task).apply { callbackLock.withLock { callbacks.add(this) } }
  }

  override suspend fun start() = progress.start()

  override suspend fun stop() {
    callbackLock.withLock { callbacks.forEach { it.stop() } }
    progress.stop()
  }
}
