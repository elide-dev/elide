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
