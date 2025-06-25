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

package dev.elide.intellij.project

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideExecutionSettings

/**
 * A service capable of using the Elide manifest and lockfile to build a project model that can be understood by the
 * IDE. Generally, the model can be built without calling the Elide CLI; however, in cases where the lockfile is out of
 * date, or dependencies are not installed, a command invocation will take place in a background task.
 *
 * The resolver is part of the auto-import feature, providing the actual import logic for the [ElideProjectAware]
 * tracker.
 */
class ElideProjectResolver : ExternalSystemProjectResolver<ElideExecutionSettings> {
  override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean = true

  override fun resolveProjectInfo(
    id: ExternalSystemTaskId,
    projectPath: String,
    isPreviewMode: Boolean,
    settings: ElideExecutionSettings?,
    listener: ExternalSystemTaskNotificationListener
  ): DataNode<ProjectData?>? {
    LOG.debug("Resolving project at '$projectPath'")

    // stubbed project model
    val projectData = ProjectData(
      Constants.SYSTEM_ID,
      "Elide Project",
      projectPath,
      projectPath,
    )

    // stubbed sample module
    val module = ModuleData(
      "sample",
      Constants.SYSTEM_ID,
      "JAVA_MODULE",
      "Sample",
      "$projectPath/src/main",
      "$projectPath/src/main",
    )

    val projectNode = DataNode(ProjectKeys.PROJECT, projectData, null)
    val moduleNode = projectNode.createChild(ProjectKeys.MODULE, module)

    // stubbed content root
    val rootData = ContentRootData(
      Constants.SYSTEM_ID,
      "$projectPath/src/main",
    )

    rootData.storePath(ExternalSystemSourceType.SOURCE, "$projectPath/src/main/kotlin")
    moduleNode.createChild(ProjectKeys.CONTENT_ROOT, rootData)

    return projectNode
  }

  private companion object {
    @JvmStatic private val LOG = Logger.getInstance(ElideProjectResolver::class.java)
  }
}
