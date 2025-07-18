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
