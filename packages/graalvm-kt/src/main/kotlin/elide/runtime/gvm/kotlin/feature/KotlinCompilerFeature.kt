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

package elide.runtime.gvm.kotlin.feature

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess

/**
 * ## Kotlin Compiler Feature
 *
 * Configures the Native Image build to be aware of Kotlin language resources that must be installed alongside Elide at
 * runtime to support compilation of Kotlin source.
 */
@Suppress("unused") internal class KotlinCompilerFeature : Feature {
  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    // Nothing yet.
  }
}
