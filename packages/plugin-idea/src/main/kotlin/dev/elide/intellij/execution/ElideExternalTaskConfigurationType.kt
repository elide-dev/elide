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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideSettings
import javax.swing.Icon

/** Extension providing the [ElideRunConfiguration] type. */
class ElideExternalTaskConfigurationType : AbstractExternalSystemTaskConfigurationType(Constants.SYSTEM_ID) {
  override fun getIcon(): Icon {
    return Constants.Icons.RELOAD_PROJECT
  }

  override fun getConfigurationFactoryId(): String = "Elide"
  override fun isDumbAware(): Boolean = true
  override fun isEditableInDumbMode(): Boolean = true

  override fun doCreateConfiguration(
    externalSystemId: ProjectSystemId,
    project: Project,
    factory: ConfigurationFactory,
    name: String
  ): ExternalSystemRunConfiguration {
    val defaultPath = ElideSettings.getSettings(project).linkedProjectsSettings.firstOrNull()?.externalProjectPath
    return ElideRunConfiguration(project, factory, name).apply {
      settings.externalProjectPath = defaultPath ?: project.basePath
    }
  }
}
