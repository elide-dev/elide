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
import com.intellij.openapi.externalSystem.util.PaintAwarePanel

/**
 * UI manager for [system-wide][ElideSettings] Elide settings panel.
 *
 * @see ElideConfigurable
 */
class ElideSystemSettingsControl : ExternalSystemSettingsControl<ElideSettings> {
  override fun isModified(): Boolean {
    return false
  }

  override fun fillUi(panel: PaintAwarePanel, indent: Int) {
    // noop
  }

  override fun reset() {
    // noop
  }

  override fun apply(settings: ElideSettings) {
    // noop
  }

  override fun validate(settings: ElideSettings): Boolean {
    return true
  }

  override fun disposeUIResources() {
    // noop
  }

  override fun showUi(visible: Boolean) {
    // noop
  }
}
