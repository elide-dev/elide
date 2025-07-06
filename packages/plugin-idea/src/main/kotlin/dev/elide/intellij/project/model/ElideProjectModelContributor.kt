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

package dev.elide.intellij.project.model

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import java.nio.file.Path
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet

/**
 * An extension used by the [ElideProjectModel] builder to enhance project data during resolution. Project model
 * contributors can add library dependencies, configure SDKs, and attach custom data to a project or module.
 */
interface ElideProjectModelContributor {
  suspend fun enhanceProject(
    projectNode: DataNode<ProjectData>,
    elideProject: ElideConfiguredProject,
    projectPath: Path,
  ) {
    // noop
  }

  suspend fun enhanceModule(
    moduleNode: DataNode<ModuleData>,
    elideProject: ElideConfiguredProject,
    elideSourceSet: SourceSet,
    projectPath: Path,
  ) {
    // noop
  }

  companion object {
    @JvmStatic val Extensions =
      ExtensionPointName.create<ElideProjectModelContributor>("dev.elide.intellij.projectModelContributor")
  }
}
