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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autolink.forEachExtensionSafeAsync
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.io.toCanonicalPath
import dev.elide.intellij.Constants
import dev.elide.intellij.project.data.ElideEntrypointInfo
import dev.elide.intellij.project.data.ElideProjectData
import dev.elide.intellij.project.model.ElideProjectModel.SOURCE_PATH_GLOB_CHAR
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetType

/** A collection of functions used to map Elide's manifest and lockfile data to Intellij's project model. */
@Suppress("UnstableApiUsage")
data object ElideProjectModel {
  @JvmStatic private val LOG = Logger.getInstance(ElideProjectModel::class.java)

  private const val SOURCE_PATH_GLOB_CHAR = '*'
  private const val SOURCE_PATH_SEPARATOR = '/'

  /**
   * Given a source set content spec, extract the content path for the equivalent intellij content root.
   *
   * This function uses a simple heuristic: a subpath from the start until the last segment that does not contain a
   * [SOURCE_PATH_GLOB_CHAR] will be selected, as it is usually the directory that contains all desired sources.
   *
   * For example, for an Elide source set defined with the pattern `"src/main/kotlin/**/*.kt"`, the returned path will
   * be "src/main/kotlin".
   */
  fun extractSourceSetContentRoot(spec: String): String {
    return spec.splitToSequence(SOURCE_PATH_SEPARATOR)
      .takeWhile { !it.contains(SOURCE_PATH_GLOB_CHAR) }
      .joinToString(separator = SOURCE_PATH_SEPARATOR.toString())
  }

  /**
   * Given a source set and its associated content root, select a path containing [contentRoot] that will be used as
   * the content root's "root" path, that is, the path that contains all content paths in that root.
   *
   * As a heuristic, this function searches for a parent of [contentRoot] with the same name as the [sourceSet]; if
   * no such path exists, the [contentRoot] is returned.
   *
   * For example, for an Elide source set named "main", with a content root at `"src/main/kotlin"`, the
   * returned path will be "src/main"; if the content root is `"src/other/kotlin"`, that same path will be returned
   */
  fun selectSourceSetRoot(contentRoot: Path, sourceSet: SourceSet): Path {
    for (i in contentRoot.nameCount - 1 downTo 0) {
      if (contentRoot.getName(i).pathString == sourceSet.name) return contentRoot.subpath(0, i + 1)
    }

    return contentRoot
  }

  /** Maps an Elide [SourceSetType] to its corresponding [ExternalSystemSourceType]. */
  fun mapSourceSetType(type: SourceSetType): ExternalSystemSourceType = when (type) {
    SourceSetType.Sources -> ExternalSystemSourceType.SOURCE
    SourceSetType.Tests -> ExternalSystemSourceType.TEST
  }

  /**
   * Build the project model given a configured Elide [elideProject]. The following mapping operations are performed:
   *
   * - The project's name is set to the one in the manifest.
   * - For each source set defined in the manifest, an Intellij [ModuleData] node is created, with a content root
   *   created from the source set's definition. The source set's type determines the type of content root created,
   *   and a module dependency on the "main" modules is automatically added to all "test" modules.
   * - Project model extensions are called to provide additional metadata for the project and its modules, such as
   *   library dependencies.
   */
  suspend fun buildProjectModel(projectPath: Path, elideProject: ElideConfiguredProject): DataNode<ProjectData> {
    // stubbed project model
    val projectData = ProjectData(
      /* owner = */ Constants.SYSTEM_ID,
      /* externalName = */ elideProject.manifest.name ?: Constants.Strings["project.defaults.name"],
      /* ideProjectFileDirectoryPath = */ projectPath.pathString,
      /* linkedExternalProjectPath = */ projectPath.pathString,
    )

    elideProject.sourceSets.find(SourceSetType.Sources)

    val projectNode = DataNode(ProjectKeys.PROJECT, projectData, null)

    val rootModule = ModuleData(
      /* id = */ projectData.id,
      /* owner = */ Constants.SYSTEM_ID,
      /* moduleTypeId = */ "EMPTY_MODULE",
      /* externalName = */ projectData.externalName,
      /* moduleFileDirectoryPath = */ projectPath.resolve(".idea").toCanonicalPath(),
      /* externalConfigPath = */ projectPath.resolve(Constants.MANIFEST_NAME).toCanonicalPath(),
    )

    val rootModuleNode = projectNode.createChild(ProjectKeys.MODULE, rootModule)

    // add the `.dev` directory as an excluded root
    val outputDir = projectPath.resolve(Constants.OUTPUT_DIR).pathString
    val outputRoot = ContentRootData(Constants.SYSTEM_ID, outputDir)
    outputRoot.storePath(ExternalSystemSourceType.EXCLUDED, outputDir)
    rootModuleNode.createChild(ProjectKeys.CONTENT_ROOT, outputRoot)

    val mainModules = elideProject.sourceSets.find(SourceSetType.Sources).toList().map {
      buildModuleDataFromSourceSet(projectNode, elideProject, it, projectPath)
    }
    LOG.info("Registered ${mainModules.size} source modules.")

    val testModules = elideProject.sourceSets.find(SourceSetType.Tests).toList().map {
      buildModuleDataFromSourceSet(projectNode, elideProject, it, projectPath, mainModules)
    }
    LOG.info("Registered ${testModules.size} test modules.")

    ElideProjectModelContributor.Extensions.forEachExtensionSafeAsync {
      it.enhanceProject(projectNode, elideProject, projectPath)
    }

    val elideData = collectElideProjectData(elideProject)
    projectNode.createChild(ElideProjectData.PROJECT_KEY, elideData)

    return projectNode
  }

  /** Build an Intellij project module for the given Elide source set. */
  private suspend fun buildModuleDataFromSourceSet(
    projectNode: DataNode<ProjectData>,
    elideProject: ElideConfiguredProject,
    elideSourceSet: SourceSet,
    projectPath: Path,
    moduleDependencies: Iterable<DataNode<ModuleData>>? = null,
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

    val moduleNode = projectNode.createChild(ProjectKeys.MODULE, module)

    // map content roots
    elideSourceSet.spec.forEach { spec ->
      val storePath = Path(extractSourceSetContentRoot(spec))
      val rootPath = selectSourceSetRoot(storePath, elideSourceSet)

      val sourceRootData = ContentRootData(Constants.SYSTEM_ID, projectPath.resolve(rootPath).pathString)
      sourceRootData.storePath(mapSourceSetType(elideSourceSet.type), projectPath.resolve(storePath).pathString)

      moduleNode.createChild(ProjectKeys.CONTENT_ROOT, sourceRootData)
    }

    // add module dependencies
    moduleDependencies?.forEach {
      moduleNode.createChild(ProjectKeys.MODULE_DEPENDENCY, ModuleDependencyData(module, it.data))
    }

    ElideProjectModelContributor.Extensions.forEachExtensionSafeAsync {
      it.enhanceModule(moduleNode, elideProject, elideSourceSet, projectPath)
    }

    return moduleNode
  }

  private fun collectElideProjectData(elideProject: ElideConfiguredProject): ElideProjectData {
    val entrypoints = buildList {
      // scripts can be used as tasks
      elideProject.manifest.scripts.forEach { (name, script) ->
        add(ElideEntrypointInfo.script(name))
      }

      // explicit entry points
      elideProject.manifest.entrypoint?.forEach { entrypoint ->
        add(ElideEntrypointInfo.generic(entrypoint))
      }

      // JVM main class
      elideProject.manifest.jvm?.main?.let { mainClassName ->
        add(ElideEntrypointInfo.jvmMain(mainClassName))
      }
    }

    return ElideProjectData(entrypoints)
  }
}
