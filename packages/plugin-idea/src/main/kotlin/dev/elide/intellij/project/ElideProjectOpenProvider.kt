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

import com.intellij.openapi.application.EDT
import com.intellij.openapi.externalSystem.action.DetachExternalProjectAction
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.trusted.ExternalSystemTrustedProjectDialog
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vfs.VirtualFile
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideProjectSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Service used to link an Elide project with the IDE, enabling auto-import, sync, and other features. */
@Suppress("UnstableApiUsage") class ElideOpenProjectProvider : AbstractOpenProjectProvider() {
  override val systemId: ProjectSystemId = Constants.SYSTEM_ID

  override fun isProjectFile(file: VirtualFile): Boolean = !file.isDirectory && file.name == Constants.MANIFEST_NAME

  override suspend fun linkProject(projectFile: VirtualFile, project: Project) {
    val projectPath = getProjectDirectory(projectFile).toNioPath()
    if (!ExternalSystemTrustedProjectDialog.confirmLinkingUntrustedProjectAsync(
        project = project,
        systemId = Constants.SYSTEM_ID,
        projectRoot = projectPath,
      )
    ) return

    val settings = ElideProjectSettings()
    settings.externalProjectPath = projectPath.toCanonicalPath()

    ExternalSystemUtil.linkExternalProject(
      /* externalSystemId = */ systemId,
      /* projectSettings = */ settings,
      /* project = */ project,
      /* importResultCallback = */ { },
      /* isPreviewMode = */ false,
      /* progressExecutionMode = */ ProgressExecutionMode.IN_BACKGROUND_ASYNC,
    )
  }

  override suspend fun unlinkProject(project: Project, externalProjectPath: String) {
    val projectData = ExternalSystemApiUtil.findProjectNode(project, systemId, externalProjectPath)?.data ?: return
    withContext(Dispatchers.EDT) {
      DetachExternalProjectAction.detachProject(project, projectData.owner, projectData, null)
    }
  }
}
