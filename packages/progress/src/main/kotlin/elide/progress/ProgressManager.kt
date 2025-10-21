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

/**
 * A high-level interface for managing a [Progress] animation.
 *
 * @property progress [Progress] instance managed by this progress manager.
 * @author Lauri Heino <datafox>
 */
public interface ProgressManager {
  public val progress: Progress

  /** Adds a new task to the [progress] and returns a [TaskCallback] for that task. */
  public suspend fun addTask(name: String, target: Int, status: String = ""): TaskCallback

  /** Starts rendering the progress animation. */
  public suspend fun start()

  /** [Stops][TaskCallback.stop] all [TaskCallback] flow collection jobs and stops rendering the progress animation. */
  public suspend fun stop()
}
