/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.feature

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess

/**
 * # Embedded Runtime Feature
 *
 * Configures GraalVM for use with static JNI and embedded native access to Elide dispatch.
 */
public class EmbeddedRuntime : Feature {
  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean = true
  override fun getDescription(): String = "Configures Elide for native embedding and static JNI."
  override fun getURL(): String = "https://elide.dev"

  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    // Nothing yet.
  }
}
