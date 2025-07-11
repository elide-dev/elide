package dev.elide.intellij.execution

import com.intellij.openapi.externalSystem.service.execution.configuration.ExternalSystemReifiedRunConfigurationExtension
import com.intellij.openapi.externalSystem.service.execution.configuration.addCommandLineFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.addWorkingDirectoryFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.ui.project.path.ExternalSystemWorkingDirectoryInfo
import dev.elide.intellij.Constants
import dev.elide.intellij.cli.ElideCommandLineInfo

/**
 * Extension used to customize the [ElideRunConfiguration]'s editor interface with additional fields.
 */
class ElideRunConfigurationExtension :
  ExternalSystemReifiedRunConfigurationExtension<ElideRunConfiguration>(ElideRunConfiguration::class.java) {
  override fun SettingsEditorFragmentContainer<ElideRunConfiguration>.configureFragments(
    configuration: ElideRunConfiguration
  ) {
    val project = configuration.project
    val workingDirectoryField = addWorkingDirectoryFragment(
      project = project,
      workingDirectoryInfo = ExternalSystemWorkingDirectoryInfo(project, Constants.SYSTEM_ID),
    )

    addCommandLineFragment(
      project = project,
      commandLineInfo = ElideCommandLineInfo(project, workingDirectoryField.component().component),
      getCommandLine = { rawCommandLine },
      setCommandLine = { rawCommandLine = it },
    )
  }
}
