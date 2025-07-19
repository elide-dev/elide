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
package dev.elide.intellij.action

import com.intellij.ide.actions.runAnything.RunAnythingManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import dev.elide.intellij.Constants

/** Action used to show the "run anything" UI preloaded with an "elide" search term. */
class ElideRunAction : ExternalSystemAction() {
  override fun isVisible(e: AnActionEvent): Boolean {
    if (!super.isVisible(e)) return false

    val projectsView = e.getData(ExternalSystemDataKeys.VIEW)
    return projectsView == null || getSystemId(e) == Constants.SYSTEM_ID
  }

  override fun isEnabled(e: AnActionEvent): Boolean {
    return true
  }

  override fun update(e: AnActionEvent) {
    e.presentation.apply {
      isVisible = isVisible(e)
      isEnabled = isEnabled(e)
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val runAnythingManager = RunAnythingManager.getInstance(project)

    runAnythingManager.show("${Constants.Strings["actions.runAnything.search"]} ", false, e)
  }
}
