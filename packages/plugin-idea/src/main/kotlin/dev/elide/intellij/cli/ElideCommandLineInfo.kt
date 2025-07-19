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
package dev.elide.intellij.cli

import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.service.ui.command.line.CommandLineInfo
import com.intellij.openapi.externalSystem.service.ui.command.line.CompletionTableInfo
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.externalSystem.service.ui.project.path.WorkingDirectoryField
import com.intellij.openapi.observable.util.createTextModificationTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import dev.elide.intellij.Constants
import dev.elide.intellij.service.elideProjectIndex
import dev.elide.intellij.project.model.fullCommandLine
import javax.swing.Icon

/** Extension used to provide completion suggestions and assistance for Elide run configurations. */
class ElideCommandLineInfo(
  project: Project,
  workingDirectoryField: WorkingDirectoryField
) : CommandLineInfo {
  override val fieldEmptyState: String = Constants.Strings["execution.cmdline.empty"]

  override val dialogTitle: String = Constants.Strings["execution.dialog.title"]
  override val dialogTooltip: String = Constants.Strings["execution.dialog.tooltip"]

  override val settingsHint: String = Constants.Strings["execution.settings.hint"]
  override val settingsName: String = Constants.Strings["execution.settings.name"]

  override val tablesInfo: List<CompletionTableInfo> = listOf(
    TaskCompletionTableInfo(project, workingDirectoryField),
  )

  private class TaskCompletionTableInfo(
    private val project: Project,
    private val workdirField: WorkingDirectoryField,
  ) : CompletionTableInfo {
    override val emptyState: String = Constants.Strings["execution.completion.tasks.emptyState"]

    override val dataColumnIcon: Icon = AllIcons.General.Gear
    override val dataColumnName: String = Constants.Strings["execution.completion.table.tasks.name"]

    override val descriptionColumnIcon: Icon = AllIcons.General.BalloonInformation
    override val descriptionColumnName: String = Constants.Strings["execution.completion.table.tasks.description"]

    override val completionModificationTracker: ModificationTracker = workdirField.createTextModificationTracker()

    override suspend fun collectCompletionInfo(): List<TextCompletionInfo> {
      return collectEntrypointTasks() + listOf(
        TextCompletionInfo(
          text = Constants.Strings["execution.completion.tasks.install.name"],
          description = Constants.Strings["execution.completion.tasks.install.description"],
        ),
        TextCompletionInfo(
          text = Constants.Strings["execution.completion.tasks.build.name"],
          description = Constants.Strings["execution.completion.tasks.build.description"],
        ),
        TextCompletionInfo(
          text = Constants.Strings["execution.completion.tasks.run.name"],
          description = Constants.Strings["execution.completion.tasks.run.description"],
        ),
        TextCompletionInfo(
          text = Constants.Strings["execution.completion.tasks.serve.name"],
          description = Constants.Strings["execution.completion.tasks.serve.description"],
        ),
      )
    }

    override suspend fun collectTableCompletionInfo(): List<TextCompletionInfo> {
      return collectCompletionInfo()
    }

    private fun collectEntrypointTasks(): List<TextCompletionInfo> {
      return project.elideProjectIndex[workdirField.workingDirectory]?.entrypoints?.map {
        TextCompletionInfo(
          text = it.fullCommandLine,
          description = "Run the ${it.descriptiveName}",
        )
      }.orEmpty()
    }
  }
}
