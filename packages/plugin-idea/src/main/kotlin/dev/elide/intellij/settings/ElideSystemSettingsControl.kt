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

import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import dev.elide.intellij.Constants

/**
 * UI manager for [system-wide][ElideSettings] Elide settings panel.
 *
 * @see ElideConfigurable
 */
class ElideSystemSettingsControl(
  private val initialSettings: ElideSettings
) : ExternalSystemSettingsControl<ElideSettings> {
  private lateinit var controls: DialogPanel

  // controls state
  private var downloadSources: Boolean = initialSettings.downloadSources
  private var downloadDocs: Boolean = initialSettings.downloadDocs

  private fun controlsPanel(): DialogPanel = panel {
    row {
      checkBox(Constants.Strings["settings.general.downloadSources"])
        .comment(Constants.Strings["settings.general.downloadSources.comment"])
        .bindSelected(::downloadSources)
    }

    row {
      checkBox(Constants.Strings["settings.general.downloadDocs"])
        .comment(Constants.Strings["settings.general.downloadDocs.comment"])
        .bindSelected(::downloadDocs)
    }
  }

  override fun isModified(): Boolean {
    controls.apply() // apply UI changes to bound properties before checking

    if (downloadSources != initialSettings.downloadSources) return true
    if (downloadDocs != initialSettings.downloadDocs) return true

    return false
  }

  override fun fillUi(canvas: PaintAwarePanel, indent: Int) {
    controls = controlsPanel()
    canvas.add(controls, ExternalSystemUiUtil.getFillLineConstraints(indent))
  }

  override fun reset() {
    downloadSources = initialSettings.downloadSources
    downloadDocs = initialSettings.downloadDocs

    controls.reset()
  }

  override fun apply(settings: ElideSettings) {
    // apply UI changes to bound properties before committing
    controls.apply()

    settings.downloadSources = downloadSources
    settings.downloadDocs = downloadDocs
  }

  override fun validate(settings: ElideSettings): Boolean {
    controls.apply()
    return true
  }

  override fun disposeUIResources() {
    // noop
  }

  override fun showUi(visible: Boolean) {
    controls.isVisible = visible
  }

  private companion object {
    private val LOG = com.intellij.openapi.diagnostic.Logger.getInstance(ElideSystemSettingsControl::class.java)
  }
}
