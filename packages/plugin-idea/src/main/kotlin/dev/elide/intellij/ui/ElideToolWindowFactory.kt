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
package dev.elide.intellij.ui

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideSettings

/** Provides a tool window with basic actions like "sync all projects" and a view of the project's structure */
class ElideToolWindowFactory : AbstractExternalSystemToolWindowFactory(Constants.SYSTEM_ID) {
  override fun getSettings(project: Project): AbstractExternalSystemSettings<*, *, *> {
    return ElideSettings.getSettings(project)
  }
}
