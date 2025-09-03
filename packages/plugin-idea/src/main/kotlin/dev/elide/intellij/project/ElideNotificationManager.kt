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
package dev.elide.intellij.project

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.elide.intellij.Constants

/**
 * Manages notifications related to Elide projects and suppresses unwanted Pkl notifications.
 * 
 * This service proactively prevents the Pkl plugin from showing sync banners for Elide manifest files
 * by intercepting and filtering notifications before they reach the user.
 */
@Service(Service.Level.PROJECT)
class ElideNotificationManager(private val project: Project) {

  /**
   * Determines if a notification should be suppressed based on its content and context.
   * 
   * @param notification The notification to evaluate
   * @return true if the notification should be suppressed, false otherwise
   */
  fun shouldSuppressNotification(notification: Notification): Boolean {
    val content = notification.content.lowercase()
    val title = notification.title?.lowercase() ?: ""
    
    // Check if this is a Pkl project sync notification that mentions elide.pkl
    val isPklSyncNotification = (title.contains("pkl") || content.contains("pkl")) &&
                               (title.contains("sync") || content.contains("sync") || 
                                title.contains("project") || content.contains("project"))
    
    val mentionsElideManifest = content.contains(Constants.MANIFEST_NAME.lowercase()) ||
                               content.contains("elide.pkl")
    
    if (isPklSyncNotification && mentionsElideManifest) {
      LOG.info("Suppressing Pkl sync notification for Elide manifest: title='$title', content='$content'")
      return true
    }
    
    return false
  }

  /**
   * Determines if a file path represents an Elide project manifest.
   */
  fun isElideManifest(filePath: String): Boolean {
    return filePath.endsWith("/${Constants.MANIFEST_NAME}") || 
           filePath.endsWith("\\${Constants.MANIFEST_NAME}")
  }

  companion object {
    private val LOG = Logger.getInstance(ElideNotificationManager::class.java)
    
    fun getInstance(project: Project): ElideNotificationManager {
      return project.getService(ElideNotificationManager::class.java)
    }
  }
}

/**
 * Startup activity that configures notification suppression for Pkl-related notifications
 * in Elide projects.
 */
class ElideNotificationStartupActivity : ProjectActivity {
  
  override suspend fun execute(project: Project) {
    val notificationManager = ElideNotificationManager.getInstance(project)
    
    // Register a global notification listener to suppress unwanted Pkl notifications
    ApplicationManager.getApplication().messageBus.connect(project).subscribe(
      com.intellij.notification.Notifications.TOPIC,
      object : com.intellij.notification.Notifications {
        override fun notify(notification: Notification) {
          if (notificationManager.shouldSuppressNotification(notification)) {
            // Prevent the notification from being shown
            notification.expire()
            LOG.debug("Suppressed unwanted Pkl notification for Elide project")
          }
        }
      }
    )
    
    LOG.debug("Elide notification suppression configured for project: ${project.name}")
  }
  
  private companion object {
    private val LOG = Logger.getInstance(ElideNotificationStartupActivity::class.java)
  }
}