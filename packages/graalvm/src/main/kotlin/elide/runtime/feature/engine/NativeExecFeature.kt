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
@file:Suppress("SpreadOperator", "unused", "ConstPropertyName")

package elide.runtime.feature.engine

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import kotlin.io.path.Path
import elide.annotations.engine.VMFeature

/** Registers native exec libraries for static JNI. */
@VMFeature internal class NativeExecFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private val staticJni = java.lang.Boolean.getBoolean("elide.staticJni")

    private val exec = arrayOf(
      "elide.exec.Execution",
    )

    private const val libexec = "exec"
    private val libexec_a = Path(System.getProperty("elide.natives")).resolve("lib$libexec.a")
    private val libexec_dylib = Path(System.getProperty("elide.natives")).resolve("lib$libexec.dylib")
    private val libexec_so = Path(System.getProperty("elide.natives")).resolve("lib$libexec.so")
  }

  override fun getDescription(): String = "Registers native exec libraries"

  // static mode only; otherwise, standard JNI is used
  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean = staticJni

  override fun nativeLibs(access: BeforeAnalysisAccess) = when (staticJni) {
    false -> emptyList()
    true -> listOf(
      nativeLibrary(
        darwin = libraryNamed(
          libexec,
          *exec,
          builtin = true,
          initializer = true,
          absolutePath = libexec_a,
          absoluteLibs = (libexec_a to libexec_dylib),
        ),
        linux = libraryNamed(
          libexec,
          *exec,
          builtin = true,
          initializer = true,
          absolutePath = libexec_a,
          absoluteLibs = (libexec_a to libexec_so),
        ),
      )
    )
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
  }
}
