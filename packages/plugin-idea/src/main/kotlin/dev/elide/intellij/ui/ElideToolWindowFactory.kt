package dev.elide.intellij.ui

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideSettings

/** Provides a tool window with basic actions like "sync all projects" and a view of the project's structure */
class ElideToolWindowFactory : AbstractExternalSystemToolWindowFactory(Constants.SYSTEM_ID) {
  override fun getSettings(project: Project): AbstractExternalSystemSettings<*, *, *> {
    return ElideSettings.getSettings(project)
  }
}
