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

package elide.internal.conventions.linting

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import elide.internal.conventions.Constants
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.ElideBuildExtension

private fun SpotlessExtension.configureSpotlessForProject(conventions: ElideBuildExtension, project: Project) {
  isEnforceCheck = conventions.checks.enforceCheck

  if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") ||
    project.pluginManager.hasPlugin("org.jetbrains.kotlin.js") ||
    project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")
  ) {
    kotlin {
      licenseHeaderFile(project.rootProject.layout.projectDirectory.file(".github/license-header.txt"))
      if (conventions.checks.ktlint) {
        ktlint(Versions.KTLINT).editorConfigOverride(Constants.Linting.ktlintOverrides)
      }
      if (conventions.checks.diktat) {
        diktat(Versions.DIKTAT).configFile("${project.rootDir}/config/diktat/diktat.yml")
      }
    }
  }
  kotlinGradle {
    target("*.gradle.kts")
    if (conventions.checks.ktlint) {
      ktlint(Versions.KTLINT).editorConfigOverride(Constants.Linting.ktlintOverridesKts)
    }
    if (conventions.checks.diktat) {
      diktat(Versions.DIKTAT).configFile("${project.rootDir}/config/diktat/diktat.yml")
    }
  }
}

public class SpotlessConventionsPlugin : Plugin<Project> {
  private companion object {
    const val SPOTLESS_PLUGIN = "com.diffplug.spotless"
  }

  override fun apply(target: Project) {
    target.pluginManager.withPlugin(SPOTLESS_PLUGIN) {
      val conventions = target.extensions.getByType(ElideBuildExtension::class.java)
      target.extensions.getByType(SpotlessExtension::class.java).run {
        configureSpotlessForProject(conventions, target)
      }
    }
  }
}
