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

package dev.elide.intellij.settings

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import org.jetbrains.annotations.NonNls

/**
 * A provider for settings panels used to configure Elide at the IDE and project level.
 *
 * @see ElideProjectSettingsControl
 */
class ElideConfigurable(project: Project) : AbstractExternalSystemConfigurable<
        ElideProjectSettings,
        ElideSettingsListener,
        ElideSettings,
        >(
  project, Constants.SYSTEM_ID,
) {
  override fun getId(): @NonNls String = Constants.CONFIGURABLE_ID
  override fun getDisplayName() = "Elide"

  override fun newProjectSettings(): ElideProjectSettings = ElideProjectSettings()
  override fun createProjectSettingsControl(settings: ElideProjectSettings) = ElideProjectSettingsControl(settings)
  override fun createSystemSettingsControl(settings: ElideSettings) = ElideSystemSettingsControl(settings)
}
