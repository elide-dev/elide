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

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideProjectSettings
import dev.elide.intellij.settings.ElideSettings

/**
 * Tracking service used to link Elide projects when they are opened by the IDE. This class enables the auto-import
 * and project sync features automatically, by configuring which files should be tracked by Intellij.
 */
@Suppress("UnstableApiUsage") class ElideUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  override val systemId: ProjectSystemId = Constants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean {
    return buildFile.name == Constants.MANIFEST_NAME
  }

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean {
    val settings = ElideSettings.getSettings(project)
    return settings.getLinkedProjectSettings(externalProjectPath) != null
  }

  override fun subscribe(project: Project, listener: ExternalSystemProjectLinkListener, parentDisposable: Disposable) {
    ElideSettings.getSettings(project).subscribe(
      object : ExternalSystemSettingsListener<ElideProjectSettings?> {
        override fun onProjectsLinked(settings: Collection<ElideProjectSettings?>) {
          settings.forEach { if (it != null) listener.onProjectLinked(it.externalProjectPath) }
        }

        override fun onProjectsUnlinked(linkedProjectPaths: Set<String?>) {
          linkedProjectPaths.forEach { if (it != null) listener.onProjectUnlinked(it) }
        }
      },
      parentDisposable,
    )
  }

  override suspend fun linkAndLoadProjectAsync(project: Project, externalProjectPath: String) {
    ElideOpenProjectProvider().linkToExistingProjectAsync(externalProjectPath, project)

  }

  override suspend fun unlinkProject(project: Project, externalProjectPath: String) {
    ElideOpenProjectProvider().unlinkProject(project, externalProjectPath)
  }
}
