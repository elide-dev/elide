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
