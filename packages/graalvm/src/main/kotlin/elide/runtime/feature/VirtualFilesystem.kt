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
package elide.runtime.feature

import com.google.common.jimfs.SystemJimfsFileSystemProvider
import com.oracle.svm.core.jdk.FileSystemProviderSupport
import org.graalvm.nativeimage.hosted.Feature
import elide.annotations.engine.VMFeature

/** GraalVM feature which enables reflection required for VFS (Virtual File System) services. */
@VMFeature
internal class VirtualFilesystem : FrameworkFeature {
  override fun isInConfiguration(access: Feature.IsInConfigurationAccess): Boolean {
    return (
      // the VFS interface and embedded impl must be in the classpath
      access.findClassByName("elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl") != null
    )
  }

  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    // Nothing to do.
  }

  @Suppress("DEPRECATION")
  override fun afterRegistration(access: Feature.AfterRegistrationAccess?) {
    // register jimfs
    FileSystemProviderSupport.register(SystemJimfsFileSystemProvider())
  }

  override fun getDescription(): String = "Configures guest VFS features"
}
