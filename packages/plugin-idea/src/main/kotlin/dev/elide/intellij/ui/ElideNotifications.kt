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

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.settings.ElideConfigurable

object ElideNotifications {
  fun notifyInvalidElideHome(project: Project? = null) {
    NotificationGroupManager.getInstance()
      .getNotificationGroup("Elide Notifications")
      .createNotification(Constants.Strings["elide.notifications.invalidHome.content"], NotificationType.ERROR)
      .setTitle(Constants.Strings["elide.notifications.invalidHome.title"])
      .addAction(
        object : NotificationAction(Constants.Strings["elide.notifications.invalidHome.configure"]) {
          override fun actionPerformed(e: AnActionEvent, n: Notification) {
            ShowSettingsUtil.getInstance().showSettingsDialog(e.project, ElideConfigurable::class.java)
          }
        },
      )
      .addAction(
        object : NotificationAction(Constants.Strings["elide.notifications.invalidHome.install"]) {
          override fun actionPerformed(e: AnActionEvent, n: Notification) {
            BrowserUtil.browse(Constants.INSTALL_URL)
          }
        },
      )
      .notify(project)
  }
}
