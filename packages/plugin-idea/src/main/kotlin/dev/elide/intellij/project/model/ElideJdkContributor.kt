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

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ModuleSdkData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData
import com.intellij.openapi.projectRoots.ProjectJdkTable
import java.nio.file.Path
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet

/**
 * Configures the project and module JDK automatically, using the Elide and IDE configuration.
 */
class ElideJdkContributor : ElideProjectModelContributor {
  override suspend fun enhanceProject(
    projectNode: DataNode<ProjectData>,
    elideProject: ElideConfiguredProject,
    projectPath: Path
  ) {
    // configure the project's JDK
    val jdkName = ProjectJdkTable.getInstance().allJdks.lastOrNull()?.name
    projectNode.createChild(ProjectSdkData.KEY, ProjectSdkData(jdkName))
  }

  override suspend fun enhanceModule(
    moduleNode: DataNode<ModuleData>,
    elideProject: ElideConfiguredProject,
    elideSourceSet: SourceSet,
    projectPath: Path
  ) {
    // configure the module's JDK
    val jdkName = ProjectJdkTable.getInstance().allJdks.lastOrNull()?.name
    moduleNode.createChild(ModuleSdkData.KEY, ModuleSdkData(jdkName))
  }
}
