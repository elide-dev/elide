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

/** Registers native trace libraries for static JNI. */
@VMFeature internal class NativeTraceFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private val staticJni = java.lang.Boolean.getBoolean("elide.staticJni")

    private val tracing = arrayOf(
      "elide.exec.Tracing",
      "elide.exec.TraceNative",
    )

    private const val libtrace = "trace"
    private val libtrace_a = Path(System.getProperty("elide.natives")).resolve("lib$libtrace.a")
    private val libtrace_dylib = Path(System.getProperty("elide.natives")).resolve("lib$libtrace.dylib")
    private val libtrace_so = Path(System.getProperty("elide.natives")).resolve("lib$libtrace.so")
  }

  override fun getDescription(): String = "Registers native trace libraries"

  // static mode only; otherwise, standard JNI is used
  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean = staticJni

  override fun nativeLibs(access: BeforeAnalysisAccess) = when (staticJni) {
    false -> emptyList()
    true -> listOf(
      nativeLibrary(
        darwin = libraryNamed(
          libtrace,
          *tracing,
          builtin = true,
          initializer = true,
          absolutePath = libtrace_a,
          absoluteLibs = (libtrace_a to libtrace_dylib),
        ),
        linux = libraryNamed(
          libtrace,
          *tracing,
          builtin = true,
          initializer = true,
          absolutePath = libtrace_a,
          absoluteLibs = (libtrace_a to libtrace_so),
        ),
      )
    )
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
  }
}
