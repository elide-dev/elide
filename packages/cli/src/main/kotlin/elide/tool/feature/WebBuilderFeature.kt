/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:Suppress("unused")

package elide.tool.feature

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.engine.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC
import elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature
import elide.tooling.web.WebBuilder

@VMFeature class WebBuilderFeature : AbstractStaticNativeLibraryFeature() {
  companion object {
    @JvmStatic private val staticJni = System.getProperty("elide.staticJni") == "true"

    private val libweb = arrayOf(
      "elide.tooling.web.css.CssNative",
      "elide.tooling.web.mdx.MdxNative",
    )
  }

  override fun getDescription(): String = "Embeds Elide's web builder"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return access.findClassByName("elide.tooling.web.css.CssNative") != null
  }

  @Suppress("SpreadOperator")
  override fun nativeLibs(access: BeforeAnalysisAccess): List<NativeLibInfo?> = if (staticJni) listOf(libraryNamed(
    "web",
    *libweb,
    type = STATIC,
    registerJni = true,
    builtin = true,
    eager = true,
  )) else emptyList()

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    WebBuilder.load()
  }
}
