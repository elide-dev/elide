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
package elide.exec

import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

@Serializable
public sealed interface RootTask : Task {
  public data object RootTaskId : TaskId {
    override fun compareTo(other: TaskId): Int = 0
  }

  public data object Default : RootTask {
    private val rootJob by lazy { Job() }
    override val id: TaskId get() = RootTaskId
    override val action: Action get() = Action.Inert
    override val dependencies: Dependencies get() = Dependencies.Implied
    override val inputs: Inputs get() = Inputs.None
    override val outputs: Outputs get() = Outputs.None
    override val status: Status get() = Status.READY
    override val job: Job get() = rootJob
    override fun transition(to: Status) {
      error("Cannot transition root task")
    }
    override suspend fun executeTask(scope: ActionScope): Job = rootJob
    override fun describe(): String = "Building project..."
    override fun exceptionOrNull(): Throwable? = null
  }

  override val id: TaskId get() = RootTaskId
}
