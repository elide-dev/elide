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

import com.oracle.svm.core.jdk.NativeLibrarySupport
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport
import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl
import com.oracle.svm.hosted.c.NativeLibraries
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import elide.runtime.feature.NativeLibraryFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.SHARED
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC


/**
 * TBD.
 */
internal abstract class AbstractStaticNativeLibraryFeature : NativeLibraryFeature {
  /**
   * TBD.
   */
  protected fun libraryNamed(name: String): NativeLibInfo = NativeLibInfo.of(name)

  /**
   * TBD.
   */
  protected fun nativeLibrary(
    darwin: NativeLibInfo? = null,
    linux: NativeLibInfo? = null,
    windows: NativeLibInfo? = null,
  ): NativeLibInfo? {
    return when (val os = System.getProperty("os.name", "unknown").lowercase().trim()) {
      "mac os x" -> darwin
      "linux" -> linux
      "windows" -> windows
      else -> error("unknown os: $os")
    }
  }

  /**
   * TBD.
   */
  abstract fun nativeLibs(access: BeforeAnalysisAccess): List<NativeLibInfo?>

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)

    nativeLibs(access).forEach {
      if (it == null) return@forEach  // not supported on this platform

      // register lib
      NativeLibrarySupport.singleton().preregisterUninitializedBuiltinLibrary(it.name)
      if (it.registerPrefix) {
        PlatformNativeLibrarySupport.singleton().addBuiltinPkgNativePrefix(it.prefix)
      }
      if (it.registerJni) (access as BeforeAnalysisAccessImpl).nativeLibraries.let { nativeLibraries ->
        when (it.type) {
          STATIC -> nativeLibraries.addStaticJniLibrary(it.name)
          SHARED -> error("Dynamic native libraries not supported")
        }
      }
    }
  }
}
