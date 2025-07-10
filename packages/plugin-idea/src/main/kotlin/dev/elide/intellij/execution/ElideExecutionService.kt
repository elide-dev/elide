package dev.elide.intellij.execution

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants

/** Project service used to invoke the [dev.elide.intellij.cli.ElideCommandLine] when executing external tasks. */
@Service(Service.Level.PROJECT)
class ElideExecutionService(private val project: Project) {
  fun execute(
    fullCommandLine: String,
    externalProjectPath: String,
    executor: Executor?,
  ) {
    val settings = ExternalSystemTaskExecutionSettings()
    settings.taskNames = listOf(fullCommandLine.trim())
    settings.externalProjectPath = externalProjectPath
    settings.externalSystemIdString = Constants.SYSTEM_ID.toString()

    val taskInfo = ExternalTaskExecutionInfo(settings, executor?.id ?: DefaultRunExecutor.EXECUTOR_ID)

    ExternalSystemUtil.runTask(
      /* taskSettings = */ taskInfo.settings,
      /* executorId = */ taskInfo.executorId,
      /* project = */ project,
      /* externalSystemId = */ Constants.SYSTEM_ID,
      /* callback = */ null,
      /* progressExecutionMode = */ ProgressExecutionMode.NO_PROGRESS_ASYNC,
    )

    val configuration = ExternalSystemUtil.createExternalSystemRunnerAndConfigurationSettings(
      /* taskSettings = */ taskInfo.settings,
      /* project = */ project,
      /* externalSystemId = */ Constants.SYSTEM_ID,
    ) ?: return

    val runManager = RunManager.getInstance(project)
    val existingConfiguration = runManager.findConfigurationByTypeAndName(configuration.type, configuration.name)

    if (existingConfiguration == null) runManager.setTemporaryConfiguration(configuration)
    else runManager.selectedConfiguration = existingConfiguration
  }
}
