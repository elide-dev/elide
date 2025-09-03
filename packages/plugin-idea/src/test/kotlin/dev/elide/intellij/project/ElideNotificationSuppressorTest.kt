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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.elide.intellij.Constants
import io.mockk.*

class ElideNotificationSuppressorTest : BasePlatformTestCase() {
  
  private lateinit var suppressor: ElideNotificationSuppressor
  
  override fun setUp() {
    super.setUp()
    suppressor = ElideNotificationSuppressor()
  }
  
  fun testSuppressesNotificationForElideManifest() {
    val elideManifestFile = mockk<VirtualFile> {
      every { name } returns Constants.MANIFEST_NAME
      every { path } returns "/project/elide.pkl"
      every { parent } returns null
    }
    
    val result = suppressor.isNotificationApplicable(project, elideManifestFile)
    
    assertFalse("Should suppress notification for elide.pkl file", result)
  }
  
  fun testSuppressesNotificationForFileInElideProject() {
    val elideManifest = mockk<VirtualFile> {
      every { name } returns Constants.MANIFEST_NAME
    }
    
    val parentDir = mockk<VirtualFile> {
      every { findChild(Constants.MANIFEST_NAME) } returns elideManifest
    }
    
    val pklFileInElideProject = mockk<VirtualFile> {
      every { name } returns "other.pkl"
      every { path } returns "/project/src/other.pkl"
      every { parent } returns parentDir
    }
    
    val result = suppressor.isNotificationApplicable(project, pklFileInElideProject)
    
    assertFalse("Should suppress notification for .pkl file in Elide project", result)
  }
  
  fun testAllowsNotificationForStandalonePklFile() {
    val parentDir = mockk<VirtualFile> {
      every { findChild(Constants.MANIFEST_NAME) } returns null
      every { parent } returns null
    }
    
    val standalonePklFile = mockk<VirtualFile> {
      every { name } returns "standalone.pkl"
      every { path } returns "/other-project/standalone.pkl"
      every { parent } returns parentDir
    }
    
    val result = suppressor.isNotificationApplicable(project, standalonePklFile)
    
    assertTrue("Should allow notification for standalone .pkl file", result)
  }
  
  fun testReturnsEmptyActionsForElideFiles() {
    val elideManifestFile = mockk<VirtualFile> {
      every { name } returns Constants.MANIFEST_NAME
      every { parent } returns null
    }
    
    val actions = suppressor.getNotificationActions(project, elideManifestFile)
    
    assertTrue("Should return empty actions for Elide manifest", actions.isEmpty())
  }
}