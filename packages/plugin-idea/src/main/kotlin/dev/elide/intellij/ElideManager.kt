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

package dev.elide.intellij

import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.ExternalSystemManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.util.Function
import dev.elide.intellij.project.ElideProjectResolver
import dev.elide.intellij.settings.*
import dev.elide.intellij.tasks.ElideTaskManager
import java.io.File

/**
 * Coordinator service for Elide as an external build system.
 *
 * The Elide Manager allows the IDE to discover the plugin components that should interact with the project model, and
 * provides general structural information about an Elide project:
 *
 *  - The [ElideProjectResolver] service builds the project model from configuration files.
 *  - The [ElideTaskManager] service handles long-running, background operations.
 *  - The [ElideSettings] class manages global configuration for the plugin at the IDE level.
 *  - The [ElideLocalSettings] class manages user-local configuration.
 *  - The [ElideExecutionSettings] container is used to collect the above settings and pass them to services like the
 *    resolver or the task manager.
 *
 * Together, these components provide the main plugin features, such as auto-import, project discovery, sync, build
 * actions, etc. Some additional parts, such as the [dev.elide.intellij.startup.ElideStartupSyncActivity], are used
 * to complement those features and improve the experience (e.g. by scanning for a project on startup).
 */
class ElideManager : ExternalSystemAutoImportAware, ExternalSystemManager<
        ElideProjectSettings,
        ElideSettingsListener,
        ElideSettings,
        ElideLocalSettings,
        ElideExecutionSettings,
        > {

  override fun getSystemId(): ProjectSystemId = Constants.SYSTEM_ID

  override fun getSettingsProvider(): Function<Project, ElideSettings> = Function { project ->
    project.getService(ElideSettings::class.java)
  }

  override fun getLocalSettingsProvider(): Function<Project, ElideLocalSettings> = Function { project ->
    project.getService(ElideLocalSettings::class.java)
  }

  override fun getExecutionSettingsProvider(): Function<Pair<Project, String>, ElideExecutionSettings> = Function {
    val project = it.first
    val path = it.second

    LOG.debug("Preparing execution settings for project at '$path': $project")
    ElideExecutionSettings()
  }

  override fun getProjectResolverClass(): Class<out ExternalSystemProjectResolver<ElideExecutionSettings>> {
    return ElideProjectResolver::class.java
  }

  override fun getTaskManagerClass(): Class<out ExternalSystemTaskManager<ElideExecutionSettings>> {
    return ElideTaskManager::class.java
  }

  override fun getExternalProjectDescriptor(): FileChooserDescriptor = Constants.projectFileChooser()

  override fun enhanceRemoteProcessing(params: SimpleJavaParameters) {
    // noop
  }

  override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? {
    val file = File(changedFileOrDirPath)

    return when {
      // If the changed file is the manifest, return its parent directory
      file.name == Constants.MANIFEST_NAME && file.isFile -> file.parent

      // If it's a directory, check if it contains elide.pkl
      file.isDirectory && File(file, Constants.MANIFEST_NAME).exists() -> changedFileOrDirPath

      // Check parent directories up to a reasonable limit
      else -> {
        var current = if (file.isDirectory) file else file.parentFile
        var depth = 0

        while (current != null && depth < 5) {
          if (File(current, Constants.MANIFEST_NAME).exists()) break

          current = current.parentFile
          depth++
        }

        current?.absolutePath
      }
    }
  }

  override fun getAffectedExternalProjectFiles(projectPath: String, project: Project): List<File?> {
    return File(projectPath, Constants.MANIFEST_NAME)
      .takeIf { it.exists() }
      ?.let { listOf(it) }
      .orEmpty()
  }

  private companion object {
    @JvmStatic private val LOG = Logger.getInstance(ElideManager::class.java)
  }
}
