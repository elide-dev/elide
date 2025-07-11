package dev.elide.intellij.project.data

import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideSettings

data class ElideProjectData(
  val entrypoints: List<ElideEntrypointInfo>,
) {
  companion object {
    @Suppress("UnstableApiUsage") fun load(project: Project): ElideProjectDataMap {
      return CachedValuesManager.getManager(project).getCachedValue(project) {
        CachedValueProvider.Result.create(findElideData(project), ExternalProjectsDataStorage.getInstance(project))
      }
    }

    private fun findElideData(project: Project): ElideProjectDataMap = buildMap {
      val projectDataManager = ProjectDataManager.getInstance()
      for (settings in ElideSettings.getSettings(project).linkedProjectsSettings) {
        val projectNode = projectDataManager.getExternalProjectData(
          /* project = */ project,
          /* projectSystemId = */ Constants.SYSTEM_ID,
          /* externalProjectPath = */ settings.externalProjectPath,
        )?.externalProjectStructure ?: continue

        val elideNode = ExternalSystemApiUtil.getChildren(projectNode, ElideProjectKeys.ELIDE_DATA).firstOrNull()
          ?: continue

        put(settings.externalProjectPath, elideNode.data)
      }
    }
  }
}
