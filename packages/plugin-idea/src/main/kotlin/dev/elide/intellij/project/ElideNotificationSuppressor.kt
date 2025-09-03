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
import com.intellij.notification.NotificationAction
import com.intellij.notification.impl.NotificationGroupManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autolink.UnlinkedProjectNotificationAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.elide.intellij.Constants
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * Suppresses Pkl project sync notifications for Elide manifest files.
 * 
 * This class prevents the Pkl plugin from showing sync banners when editing `elide.pkl` files,
 * since these are Elide project manifests, not standalone Pkl projects.
 */
class ElideNotificationSuppressor : UnlinkedProjectNotificationAware {
  
  override fun getNotificationActions(project: Project, projectFile: VirtualFile): List<NotificationAction> {
    // Don't provide any notification actions for Elide-related files
    if (isElideRelatedFile(projectFile)) {
      return emptyList()
    }
    
    // Return default actions for other files
    return super.getNotificationActions(project, projectFile)
  }

  override fun isNotificationApplicable(project: Project, projectFile: VirtualFile): Boolean {
    // Suppress notifications for Elide-related files
    if (isElideRelatedFile(projectFile)) {
      LOG.debug("Suppressing Pkl sync notification for Elide-related file: ${projectFile.path}")
      return false
    }
    
    // Allow normal Pkl project notifications for other .pkl files
    return true
  }
  
  /**
   * Determines if a file is related to an Elide project and should not trigger Pkl sync notifications.
   */
  private fun isElideRelatedFile(file: VirtualFile): Boolean {
    // Check if this is an Elide manifest file
    if (file.name == Constants.MANIFEST_NAME) {
      return true
    }
    
    // Check if this file is in a directory containing an Elide manifest
    val parentDir = file.parent
    if (parentDir != null) {
      val elideManifest = parentDir.findChild(Constants.MANIFEST_NAME)
      if (elideManifest != null) {
        return true
      }
      
      // Check parent directories up to 3 levels up for Elide manifests
      var currentDir = parentDir.parent
      var level = 0
      while (currentDir != null && level < 3) {
        if (currentDir.findChild(Constants.MANIFEST_NAME) != null) {
          return true
        }
        currentDir = currentDir.parent
        level++
      }
    }
    
    return false
  }

  private companion object {
    private val LOG = Logger.getInstance(ElideNotificationSuppressor::class.java)
  }
}