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
@file:Suppress("unused")

package elide.tool.feature

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.engine.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC
import elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature

@VMFeature class LocalInferenceFeature : AbstractStaticNativeLibraryFeature() {
  companion object {
    @JvmStatic private val staticJni = System.getProperty("elide.staticJni") == "true"
  }

  override fun getDescription(): String = "Enables local inference features via llama.cpp"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return access.findClassByName("elide.runtime.localai.NativeLocalAi") != null
  }

  override fun nativeLibs(access: BeforeAnalysisAccess): List<NativeLibInfo?> = if (staticJni) listOf(libraryNamed(
    "local_ai",
    "elide.runtime.localai.NativeLocalAi",
    type = STATIC,
  )) else emptyList()
}
