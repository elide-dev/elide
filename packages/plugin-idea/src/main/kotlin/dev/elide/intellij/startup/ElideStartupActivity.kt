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

package dev.elide.intellij.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.util.registry.Registry
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideProjectSettings

/** Startup activity used to detect an Elide project and sync it if needed. */
class ElideStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    // request to run external system APIs in-process, as opposed to calling them in a separate background process;
    // this simplifies the setup of components like the project resolver, and allows them to use the full intellij API
    Registry.get(Constants.SYSTEM_ID.id + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX)
      .setValue(true)

    for (baseDir in project.getBaseDirectories()) {
      LOG.debug("Searching for Elide manifest in base dir $baseDir")
      baseDir.findChild(Constants.MANIFEST_NAME) ?: continue

      // have the IDE track changes to the project config files, then trigger a sync
      LOG.debug("Found manifest, linking project")
      val settings = project.getService(ElideProjectSettings::class.java)
      settings.externalProjectPath = baseDir.toNioPath().toCanonicalPath()

      ExternalSystemUtil.linkExternalProject(
        /* externalSystemId = */ Constants.SYSTEM_ID,
        /* projectSettings = */ settings,
        /* project = */ project,
        /* importResultCallback = */ { },
        /* isPreviewMode = */ false,
        /* progressExecutionMode = */ ProgressExecutionMode.IN_BACKGROUND_ASYNC,
      )
      
      break
    }
  }

  private companion object {
    @JvmStatic private val LOG = Logger.getInstance(ElideStartupActivity::class.java)
  }
}
