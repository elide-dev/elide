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

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.externalSystem.util.PaintAwarePanel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selectedValueIs
import dev.elide.intellij.Constants
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * UI manager for [project-level][ElideProjectSettings] Elide settings panel.
 *
 * @see ElideConfigurable
 */
@Suppress("UnstableApiUsage") class ElideProjectSettingsControl(
  initialSettings: ElideProjectSettings
) : AbstractExternalProjectSettingsControl<ElideProjectSettings>(initialSettings) {
  private lateinit var projectControls: DialogPanel

  private var distributionType: ElideDistributionSetting = initialSettings.elideDistributionType
  private var distributionPath: String = initialSettings.elideDistributionPath

  private fun controlsPanel(): DialogPanel = panel {
    group(Constants.Strings["settings.project.execution.title"]) {
      row(Constants.Strings["settings.project.distribution.label"]) {
        val distributionTypeBox = comboBox(ElideDistributionSetting.entries, DistributionTypeRenderer)
          .bindItem(::distributionType) { distributionType = it ?: ElideDistributionSetting.AutoDetect }

        textFieldWithBrowseButton(Constants.sdkFileChooser(), null) { it.path }
          .bindText(getter = { distributionPath }, setter = { distributionPath = it })
          .visibleIf(distributionTypeBox.component.selectedValueIs(ElideDistributionSetting.Custom))

        rowComment(Constants.Strings["settings.project.distribution.comment"])
      }
    }
  }

  override fun fillExtraControls(canvas: PaintAwarePanel, indent: Int) {
    projectControls = controlsPanel()
    canvas.add(projectControls, ExternalSystemUiUtil.getFillLineConstraints(indent))
  }

  override fun showUi(show: Boolean) {
    super.showUi(show)
    projectControls.isVisible = show
  }

  override fun isExtraSettingModified(): Boolean {
    projectControls.apply()

    if (distributionPath != initialSettings.elideDistributionPath) return true
    if (distributionType != initialSettings.elideDistributionType) return true

    return false
  }

  override fun resetExtraSettings(isDefaultModuleCreation: Boolean) {
    distributionPath = initialSettings.elideDistributionPath
    distributionType = initialSettings.elideDistributionType

    projectControls.reset()
  }

  override fun updateInitialExtraSettings() {
    projectControls.apply()

    initialSettings.elideDistributionPath = distributionPath
    initialSettings.elideDistributionType = distributionType
  }

  override fun applyExtraSettings(settings: ElideProjectSettings) {
    projectControls.apply()

    settings.elideDistributionPath = distributionPath
    settings.elideDistributionType = distributionType
  }

  override fun validate(settings: ElideProjectSettings): Boolean {
    projectControls.apply()
    return true
  }

  private data object DistributionTypeRenderer : ListCellRenderer<ElideDistributionSetting?> {
    override fun getListCellRendererComponent(
      list: JList<out ElideDistributionSetting>,
      value: ElideDistributionSetting?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val text = when (value) {
        ElideDistributionSetting.Custom -> Constants.Strings["settings.project.distribution.type.custom"]
        else -> Constants.Strings["settings.project.distribution.type.auto"]
      }

      return JLabel(text)
    }
  }
}
