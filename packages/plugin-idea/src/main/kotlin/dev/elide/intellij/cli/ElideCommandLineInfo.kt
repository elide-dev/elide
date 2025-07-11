package dev.elide.intellij.cli

import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.service.ui.command.line.CommandLineInfo
import com.intellij.openapi.externalSystem.service.ui.command.line.CompletionTableInfo
import com.intellij.openapi.externalSystem.service.ui.completion.TextCompletionInfo
import com.intellij.openapi.externalSystem.service.ui.project.path.WorkingDirectoryField
import com.intellij.openapi.observable.util.createTextModificationTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.NlsContexts
import dev.elide.intellij.Constants
import dev.elide.intellij.project.data.ElideProjectData
import dev.elide.intellij.project.data.fullCommandLine
import org.jetbrains.annotations.Nls
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
      )
    }

    override suspend fun collectTableCompletionInfo(): List<TextCompletionInfo> {
      return collectCompletionInfo()
    }

    private fun collectEntrypointTasks(): List<TextCompletionInfo> {
      val elideData = ElideProjectData.load(project)[workdirField.workingDirectory] ?: return emptyList()

      return elideData.entrypoints.map {
        TextCompletionInfo(
          text = it.fullCommandLine,
          description = "Run the ${it.descriptiveName}",
        )
      }
    }
  }
}
