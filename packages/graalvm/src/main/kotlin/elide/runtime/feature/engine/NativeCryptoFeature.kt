/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.feature.engine

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.internal.VMFeature

/** Registers native crypto libraries for static JNI. */
@VMFeature internal class NativeCryptoFeature : AbstractStaticNativeLibraryFeature() {
  override fun getDescription(): String = "Registers native crypto libraries"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return (
      access.findClassByName("io.netty.internal.tcnative.SSL") != null
    )
  }

  override fun nativeLibs(access: BeforeAnalysisAccess) = listOf(
    nativeLibrary(
      darwin = libraryNamed("tcnative_osx"),
      linux = libraryNamed("tcnative_linux"),
    )
  )
}
