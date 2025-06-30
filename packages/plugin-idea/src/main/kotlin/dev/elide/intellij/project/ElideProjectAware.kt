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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import dev.elide.intellij.Constants

/** A tracker for a project in the current IDE instance, used to trigger auto-import and sync operations. */
class ElideProjectAware(private val project: Project, private val projectPath: String) : ExternalSystemProjectAware {
  override val projectId: ExternalSystemProjectId = ExternalSystemProjectId(Constants.SYSTEM_ID, projectPath)
  override val settingsFiles: Set<String> = setOf("$projectPath/${Constants.MANIFEST_NAME}")

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    val connection = project.messageBus.connect(parentDisposable)
    connection.subscribe(TOPIC, listener)
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    val publisher = project.messageBus.syncPublisher(TOPIC)
    publisher.onProjectReloadStart()

    val callback = object : ExternalProjectRefreshCallback {
      override fun onSuccess(externalTaskId: ExternalSystemTaskId, externalProject: DataNode<ProjectData?>?) {
        publisher.onProjectReloadFinish(ExternalSystemRefreshStatus.SUCCESS)
      }

      override fun onFailure(externalTaskId: ExternalSystemTaskId, errorMessage: String, errorDetails: String?) {
        publisher.onProjectReloadFinish(ExternalSystemRefreshStatus.FAILURE)
      }
    }

    val spec = ImportSpecBuilder(project, Constants.SYSTEM_ID)
      .callback(callback)
      .activateBuildToolWindowOnStart()
      .navigateToError()
      .build()

    ExternalSystemUtil.refreshProject(projectPath, spec)
  }

  companion object {
    @JvmStatic private val LOG = Logger.getInstance(ElideProjectAware::class.java)
    @JvmStatic val TOPIC = Topic.create(
      "ElideProjectSync",
      ExternalSystemProjectListener::class.java,
      Topic.BroadcastDirection.TO_CHILDREN,
    )
  }
}
