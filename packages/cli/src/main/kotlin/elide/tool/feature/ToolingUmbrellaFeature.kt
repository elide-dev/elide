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

import dev.elide.cli.bridge.CliNativeBridge
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess.register
import elide.annotations.engine.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC
import elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature

@VMFeature class ToolingUmbrellaFeature : AbstractStaticNativeLibraryFeature() {
  companion object {
    @JvmStatic private val staticJni = System.getProperty("elide.staticJni") == "true"
  }
  override fun getDescription(): String = "Embeds Elide's native tooling umbrella library"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return access.findClassByName("dev.elide.cli.bridge.CliNativeBridge") != null
  }

  override fun nativeLibs(access: BeforeAnalysisAccess): List<NativeLibInfo?> = if (staticJni) listOf(libraryNamed(
    "umbrella",
    "dev.elide.cli.bridge.CliNativeBridge",
    type = STATIC,
    registerJni = true,
    builtin = true,
    eager = true,
  )) else emptyList()

  private fun registerJniCalls() {
    CliNativeBridge.initialize()
    register(CliNativeBridge::class.java)
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    registerJniCalls()
  }
}
