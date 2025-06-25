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
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants

/** A tracker for a project in the current IDE instance, used to trigger auto-import and sync operations. */
class ElideProjectAware(private val project: Project, projectPath: String) : ExternalSystemProjectAware {
  override val projectId: ExternalSystemProjectId = ExternalSystemProjectId(Constants.SYSTEM_ID, projectPath)
  override val settingsFiles: Set<String> = setOf("$projectPath/${Constants.MANIFEST_NAME}")

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    // notify the listener once project sync is complete
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    // noop
  }
}
