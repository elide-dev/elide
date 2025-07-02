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
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.io.toCanonicalPath
import dev.elide.intellij.Constants
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetType

data object ElideProjectModel {
  @JvmStatic private val LOG = Logger.getInstance(ElideProjectModel::class.java)

  private const val SOURCE_PATH_GLOB_CHAR = '*'
  private const val SOURCE_PATH_SEPARATOR = '/'

  fun extractSourceSetContentRoot(spec: String): String {
    return spec.splitToSequence(SOURCE_PATH_SEPARATOR)
      .takeWhile { !it.contains(SOURCE_PATH_GLOB_CHAR) }
      .joinToString(separator = SOURCE_PATH_SEPARATOR.toString())
  }

  fun selectSourceSetRoot(contentRoot: Path, sourceSet: SourceSet): Path {
    for (i in contentRoot.nameCount - 1 downTo 0) {
      if (contentRoot.getName(i).pathString == sourceSet.name) return contentRoot.subpath(0, i + 1)
    }

    return contentRoot
  }

  fun mapSourceSetType(type: SourceSetType): ExternalSystemSourceType = when (type) {
    SourceSetType.Sources -> ExternalSystemSourceType.SOURCE
    SourceSetType.Tests -> ExternalSystemSourceType.TEST
  }

  /** Build the project model given a configured Elide [elideProject]. */
  fun buildProjectModel(projectPath: Path, elideProject: ElideConfiguredProject): DataNode<ProjectData> {
    // stubbed project model
    val projectData = ProjectData(
      /* owner = */ Constants.SYSTEM_ID,
      /* externalName = */ elideProject.manifest.name ?: Constants.Strings["project.defaults.name"],
      /* ideProjectFileDirectoryPath = */ projectPath.pathString,
      /* linkedExternalProjectPath = */ projectPath.pathString,
    )

    elideProject.sourceSets.find(SourceSetType.Sources)

    val projectNode = DataNode(ProjectKeys.PROJECT, projectData, null)

    val mainModules = elideProject.sourceSets.find(SourceSetType.Sources).map {
      buildModuleDataFromSourceSet(projectNode, it, projectPath)
    }.toList()

    val testModules = elideProject.sourceSets.find(SourceSetType.Tests).map {
      buildModuleDataFromSourceSet(projectNode, it, projectPath, dependsOn = mainModules)
    }.toList()

    LOG.info("Registered ${mainModules.size} source and ${testModules.size} test modules.")
    return projectNode
  }

  fun buildModuleDataFromSourceSet(
    project: DataNode<ProjectData>,
    elideSourceSet: SourceSet,
    projectPath: Path,
    dependsOn: Iterable<DataNode<ModuleData>>? = null,
  ): DataNode<ModuleData> {
    // TODO(@darvld): account for language information in the source set instead of assuming Kotlin/Java
    // compute module info
    val module = ModuleData(
      /* id = */ elideSourceSet.name,
      /* owner = */ Constants.SYSTEM_ID,
      /* moduleTypeId = */ "JAVA_MODULE",
      /* externalName = */ elideSourceSet.name,
      /* moduleFileDirectoryPath = */ projectPath.resolve(".idea").toCanonicalPath(),
      /* externalConfigPath = */ projectPath.resolve(Constants.MANIFEST_NAME).toCanonicalPath(),
    )

    val moduleNode = project.createChild(ProjectKeys.MODULE, module)

    // map content roots
    elideSourceSet.spec.forEach { spec ->
      val storePath = Path(extractSourceSetContentRoot(spec))
      val rootPath = selectSourceSetRoot(storePath, elideSourceSet)

      val sourceRootData = ContentRootData(Constants.SYSTEM_ID, projectPath.resolve(rootPath).pathString)
      sourceRootData.storePath(mapSourceSetType(elideSourceSet.type), projectPath.resolve(storePath).pathString)

      moduleNode.createChild(ProjectKeys.CONTENT_ROOT, sourceRootData)
    }

    // add module dependencies
    dependsOn?.forEach {
      moduleNode.createChild(ProjectKeys.MODULE_DEPENDENCY, ModuleDependencyData(module, it.data))
    }

    return moduleNode
  }
}
