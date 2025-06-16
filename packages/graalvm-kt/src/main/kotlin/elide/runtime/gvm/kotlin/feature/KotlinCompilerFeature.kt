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
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "deprecation")

package elide.runtime.gvm.kotlin.feature

import com.oracle.svm.core.jdk.FileSystemProviderSupport
import jdk.internal.jrtfs.JrtFileSystemProvider
import org.graalvm.nativeimage.hosted.Feature
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import elide.runtime.feature.FrameworkFeature

/**
 * ## Kotlin Compiler Feature
 *
 * Configures the Native Image build to be aware of Kotlin language resources that must be installed alongside Elide at
 * runtime to support compilation of Kotlin source.
 */
@Suppress("unused") internal class KotlinCompilerFeature : FrameworkFeature {
  override fun getDescription(): String = "Configures the Kotlin compiler"

  @Suppress("DEPRECATION")
  @OptIn(ExperimentalCompilerApi::class)
  override fun afterRegistration(access: Feature.AfterRegistrationAccess) {
    // we need access to the JRT file system provider, but we do not want to include the build-time JDK image.
    FileSystemProviderSupport.register(JrtFileSystemProvider())
  }
}
