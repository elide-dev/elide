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

package elide.internal.conventions.linting

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import elide.internal.conventions.Constants.Versions

private fun SpotlessExtension.configureSpotlessForProject(project: Project) {
  isEnforceCheck = false

  if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") ||
    project.pluginManager.hasPlugin("org.jetbrains.kotlin.js") ||
    project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")
  ) {
    kotlin {
      licenseHeaderFile(project.rootProject.layout.projectDirectory.file(".github/license-header.txt"))
      ktlint(Versions.KTLINT).apply {
        setEditorConfigPath(project.rootProject.layout.projectDirectory.file(".editorconfig"))
      }
    }
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktlint(Versions.KTLINT)
  }
}

public class SpotlessConventionsPlugin : Plugin<Project> {
  private companion object {
    const val SPOTLESS_PLUGIN = "com.diffplug.spotless"
  }

  override fun apply(target: Project) {
    target.pluginManager.apply(SpotlessPlugin::class.java)
    target.pluginManager.withPlugin(SPOTLESS_PLUGIN) {
      target.extensions.getByType(SpotlessExtension::class.java).run {
        configureSpotlessForProject(target)
      }
    }
  }
}
