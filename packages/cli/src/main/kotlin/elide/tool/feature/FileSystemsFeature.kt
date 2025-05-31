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

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package elide.tool.feature

import com.oracle.svm.core.jdk.FileSystemProviderSupport
import jdk.nio.zipfs.ZipFileSystemProvider
import org.graalvm.nativeimage.hosted.Feature
import elide.runtime.feature.FrameworkFeature

/**
 * ## File Systems Feature
 *
 * Configures the Native Image build to support various file system providers which are required for the proper
 * operation of the Elide binary.
 */
@Suppress("unused") internal class FileSystemsFeature : FrameworkFeature {
  override fun getDescription(): String = "Configures filesystem support"

  override fun afterRegistration(access: Feature.AfterRegistrationAccess) {
    FileSystemProviderSupport.register(ZipFileSystemProvider())
  }
}
