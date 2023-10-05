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

package elide.internal.conventions.analysis

import org.sonarqube.gradle.SonarExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import elide.internal.conventions.ElideBuildExtension

// Disables code analysis features for for this project
public fun ElideBuildExtension.skipAnalysis() {
  // disable SonarQube
  project.extensions.findByType(SonarExtension::class.java)?.isSkipProject = true

  // disable Kover
  project.extensions.findByType(KoverProjectExtension::class.java)?.disable()
}
