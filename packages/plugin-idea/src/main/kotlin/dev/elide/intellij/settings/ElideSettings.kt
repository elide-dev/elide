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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project

/**
 * Global settings for Elide, managed through the IDE's settings panel. The preferences stored using this class are
 * collected along others into [ElideExecutionSettings], which are then used to control plugin features.
 *
 * @see ElideConfigurable
 */
@Service(Service.Level.PROJECT)
@State(name = "ElideSettings", storages = [Storage("elide.xml")])
class ElideSettings(
  project: Project
) : AbstractExternalSystemSettings<ElideSettings, ElideProjectSettings, ElideSettingsListener>(
  ElideSettingsListener.TOPIC,
  project,
) {
  override fun copyExtraSettingsFrom(other: ElideSettings) {
    // noop
  }

  override fun checkSettings(p0: ElideProjectSettings, p1: ElideProjectSettings) {
    // noop
  }

  override fun subscribe(
    listener: ExternalSystemSettingsListener<ElideProjectSettings?>,
    parentDisposable: Disposable
  ) {
    // noop
  }
}
