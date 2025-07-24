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
package dev.elide.intellij.tasks

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.util.io.awaitExit
import dev.elide.intellij.InvalidElideHomeException
import dev.elide.intellij.cli.ElideCommandLine
import dev.elide.intellij.cli.invoke
import dev.elide.intellij.settings.ElideExecutionSettings
import dev.elide.intellij.ui.ElideNotifications
import java.io.BufferedReader
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlin.io.path.Path

/** Background task manager for long-running operations, such as dependency sync and project builds. */
class ElideTaskManager : ExternalSystemTaskManager<ElideExecutionSettings> {
  private val runningTasks = ConcurrentHashMap<ExternalSystemTaskId, Job>()

  override fun executeTasks(
    projectPath: String,
    id: ExternalSystemTaskId,
    settings: ElideExecutionSettings,
    listener: ExternalSystemTaskNotificationListener
  ) {
    listener.onStart(projectPath, id)
    runBlocking {
      runningTasks[id] = coroutineContext.job
      try {
        val elide = ElideCommandLine.at(settings.elideHome, Path(projectPath))

        for (task in settings.tasks) {
          listener.onStatusChange(ExternalSystemTaskNotificationEvent(id, "Executing task $task"))

          val exitCode = elide({ add(task) }, settings.env) { process ->
            pipeProcessOutput(id, listener, process.inputReader(), stdout = true)
            pipeProcessOutput(id, listener, process.errorReader(), stdout = false)

            process.awaitExit()
          }

          if (exitCode == 0) listener.onSuccess(projectPath, id)
          else error("Elide invocation failed with code $exitCode")
        }
      } catch (cause: InvalidElideHomeException) {
        ElideNotifications.notifyInvalidElideHome()
        throw cause
      } finally {
        runningTasks.remove(id)
      }
    }
    listener.onEnd(projectPath, id)
  }

  override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    return runningTasks.remove(taskId)?.cancel() != null
  }

  private fun CoroutineScope.pipeProcessOutput(
    taskId: ExternalSystemTaskId,
    listener: ExternalSystemTaskNotificationListener,
    reader: BufferedReader,
    stdout: Boolean,
  ) {
    launch {
      reader.useLines { lines -> for (it in lines) listener.onTaskOutput(taskId, "$it\n", stdout) }
    }
  }
}
