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
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import dev.elide.intellij.Constants
import dev.elide.intellij.cli.ElideCommandLine
import dev.elide.intellij.cli.install
import dev.elide.intellij.manifests.ElideManifestService
import dev.elide.intellij.project.model.ElideProjectModel
import dev.elide.intellij.service.ElideDistributionResolver
import dev.elide.intellij.settings.ElideExecutionSettings
import org.jetbrains.annotations.PropertyKey
import java.nio.file.Path
import kotlinx.coroutines.launch
import kotlin.io.path.Path
import kotlin.io.path.notExists
import elide.tooling.lockfile.LockfileLoader
import elide.tooling.lockfile.loadLockfileSafe
import elide.tooling.project.ElideProjectInfo
import elide.tooling.project.ElideProjectLoader
import elide.tooling.project.SourceSetFactory

/**
 * A service capable of using the Elide manifest and lockfile to build a project model that can be understood by the
 * IDE. Generally, the model can be built without calling the Elide CLI; however, in cases where the lockfile is out of
 * date, or dependencies are not installed, a command invocation will take place in a background task.
 */
class ElideProjectResolver : ExternalSystemProjectResolver<ElideExecutionSettings> {
  private fun ExternalSystemTaskNotificationListener.onStep(taskId: ExternalSystemTaskId, text: String) {
    onStatusChange(ExternalSystemTaskNotificationEvent(taskId, text))
  }

  private fun progressMessage(@PropertyKey(resourceBundle = "i18n.Strings") key: String): String {
    return Constants.Strings["resolve.progress", Constants.Strings[key]]
  }

  override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    return true
  }

  @Suppress("UnstableApiUsage")
  override fun resolveProjectInfo(
    id: ExternalSystemTaskId,
    projectPath: String,
    isPreviewMode: Boolean,
    settings: ElideExecutionSettings?,
    resolverPolicy: ProjectResolverPolicy?,
    listener: ExternalSystemTaskNotificationListener
  ): DataNode<ProjectData>? {
    return runBlockingCancellable {
      LOG.debug("Resolving project at '$projectPath'")

      val projectModel = runCatching {
        // find a manifest in the project directory
        listener.onStep(id, progressMessage("resolve.steps.discovery"))
        val projectRoot = Path(projectPath)
        val manifestPath = projectRoot.resolve(Constants.MANIFEST_NAME)

        if (manifestPath.notExists()) {
          LOG.debug("No Elide manifest found under $projectPath")
          return@runCatching null
        }

        // parse the manifest
        listener.onStep(id, progressMessage("resolve.steps.parse"))
        val manifest = ElideManifestService().parse(manifestPath)

        // configure the project
        listener.onStep(id, progressMessage("resolve.steps.configure"))
        val loader = buildProjectLoader(projectRoot, settings)
        val project = ElideProjectInfo(projectRoot, manifest, null).load(loader)

        // call the CLI in case the lockfile is outdated
        listener.onStep(id, progressMessage("resolve.steps.refresh"))
        val elideHome = settings?.elideHome ?: ElideDistributionResolver.defaultDistributionPath()
        val cli = ElideCommandLine.at(elideHome, projectRoot)

        val exitCode = cli.install(
          withSources = settings?.downloadSources ?: true,
          withDocs = settings?.downloadDocs ?: true,
        ) { process ->
          // pass process output on to the IDE
          launch {
            process.inputStream.bufferedReader().forEachLine {
              listener.onTaskOutput(id, "$it\n", true)
            }
          }
          launch {
            process.errorStream.bufferedReader().forEachLine {
              listener.onTaskOutput(id, "$it\n", false)
            }
          }
        }

        if (exitCode != 0) error("Command 'elide install' failed with exit code $exitCode")

        // build the project model from the manifest and lockfile
        listener.onStep(id, progressMessage("resolve.steps.buildModel"))
        ElideProjectModel.buildProjectModel(projectRoot, project)
      }.onSuccess {
        listener.onSuccess(projectPath, id)
      }.onFailure { cause ->
        listener.onFailure(projectPath, id, RuntimeException("Failed to load Elide project", cause))
      }

      listener.onEnd(projectPath, id)
      projectModel.getOrThrow()
    }
  }

  /**
   * Construct a new [ElideProjectLoader] that uses the resources from the Elide distribution set in the execution
   * [settings].
   */
  private fun buildProjectLoader(
    projectPath: Path,
    settings: ElideExecutionSettings?
  ): ElideProjectLoader {
    return object : ElideProjectLoader {
      override val lockfileLoader: LockfileLoader = LockfileLoader { loadLockfileSafe(projectPath) }
      override val sourceSetFactory: SourceSetFactory = SourceSetFactory.Default
      override val resourcesPath: Path by lazy {
        val elideHome = settings?.elideHome ?: ElideDistributionResolver.defaultDistributionPath()
        ElideDistributionResolver.resourcesPath(elideHome)
      }
    }
  }

  companion object {
    @JvmStatic private val LOG = Logger.getInstance(ElideProjectResolver::class.java)
  }
}
