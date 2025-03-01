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
@file:Suppress("SpreadOperator")

package elide.runtime.lang.javascript

import org.graalvm.nativeimage.hosted.Feature
import kotlin.io.path.Path
import elide.annotations.engine.VMFeature
import elide.runtime.core.lib.NativeLibraries
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature

/**
 * ## OXC Parser Feature
 *
 * Configures native JavaScript/TypeScript parsing and compilation via Oxc.
 */
@VMFeature public class OxcParserFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private const val LIB_JS = "js"
    private val precompiler = arrayOf(
      "elide.runtime.lang.javascript.JavaScriptPrecompiler",
    )

    private val libjs_a = Path(System.getProperty("elide.natives")).resolve("lib$LIB_JS.a")
    private val libjs_so = Path(System.getProperty("elide.natives")).resolve("lib$LIB_JS.so")
    private val libjs_dylib = Path(System.getProperty("elide.natives")).resolve("lib$LIB_JS.dylib")
  }

  override fun getDescription(): String = "Configures native JavaScript/TypeScript parsing via Oxc"

  override fun nativeLibs(access: Feature.BeforeAnalysisAccess): List<NativeLibInfo?> = listOf(
    nativeLibrary(
      darwin = libraryNamed(
        LIB_JS,
        *precompiler,
        builtin = true,
        initializer = true,
        absolutePath = libjs_a,
        absoluteLibs = (libjs_a to libjs_dylib),
      ),
      linux = libraryNamed(
        LIB_JS,
        *precompiler,
        builtin = true,
        initializer = true,
        absolutePath = libjs_a,
        absoluteLibs = (libjs_a to libjs_so),
      ),
    )
  )

  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    NativeLibraries.resolve(LIB_JS) { didLoad: Boolean ->
      assert(didLoad) { "Failed to load `libjs.so` at build-time" }
    }
  }
}
