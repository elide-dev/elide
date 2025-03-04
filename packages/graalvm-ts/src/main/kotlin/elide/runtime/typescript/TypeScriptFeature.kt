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

package elide.runtime.typescript

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization
import elide.annotations.engine.VMFeature
import elide.runtime.lang.javascript.OxcParserFeature
import elide.runtime.lang.typescript.TypeScriptLanguage

/**
 * # TypeScript Feature
 *
 * Configures the GraalVM Native Image compiler to support Elide's TypeScript layer.
 */
@VMFeature public class TypeScriptFeature : Feature {
  override fun getDescription(): String = "Enables TypeScript support in Elide"

  override fun getRequiredFeatures(): List<Class<out Feature?>?>? = listOf(
    OxcParserFeature::class.java,
  )

  override fun beforeAnalysis(access: BeforeAnalysisAccess?) {
    RuntimeClassInitialization.initializeAtBuildTime(
      TypeScriptLanguage::class.java,
    )
  }
}
