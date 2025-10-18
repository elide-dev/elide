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

package elide.internal.conventions.docs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import java.io.File
import elide.internal.conventions.Constants
import elide.internal.conventions.ElideBuildExtension

private fun DokkaTask.configureDokkaForProject(conventions: ElideBuildExtension, target: Project) {
  if (conventions.docs.requested && conventions.docs.enabled) {
    val docAsset: (String) -> File = {
      target.rootProject.layout.projectDirectory.file("project/docs/$it").asFile
    }
    val creativeAsset: (String) -> File = {
      target.rootProject.layout.projectDirectory.file("project/creative/$it").asFile
    }

    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
      moduleName = "Elide API"
      moduleVersion = project.version as String
      footerMessage = "© 2023—2025 Elide Technologies, Inc."
      templatesDir = target.rootProject.layout.projectDirectory.dir("project/docs/templates").asFile
      customAssets = listOf(
        creativeAsset("logo/logo-wide-1200-w-r2.png"),
        creativeAsset("logo/gray-elide-symbol-lg.png"),
      )
      customStyleSheets = listOf(
        docAsset("styles/logo-styles.css"),
        docAsset("styles/theme-styles.css"),
      )
    }

    val projectVersion = project.version as String
    pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
      version = projectVersion
      versionsOrdering = listOf("1.0.0-beta10")
      olderVersionsDir = project.file("project/docs/versions")
      olderVersions = emptyList()
      renderVersionsNavigationOnAllPages = true
    }
  }
}

private fun DokkaTaskPartial.configureDokkaPartial() {
  if (project.layout.projectDirectory.file("module.md").asFile.exists()) {
    dokkaSourceSets {
      configureEach {
        includes.from("module.md")
      }
    }
  }
}

public class DokkaConventionsPlugin : Plugin<Project> {
  private companion object {
    const val DOKKA_PLUGIN_ID = "org.jetbrains.dokka"
  }

  override fun apply(target: Project): Unit = target.extensions.getByType(ElideBuildExtension::class.java).let {
    if (it.kotlin.requested && target.findProperty(Constants.Build.BUILD_DOCS) == "true") {
      if (it.docs.requested && it.docs.enabled) {
        target.pluginManager.apply(DokkaPlugin::class.java)
        target.pluginManager.withPlugin(DOKKA_PLUGIN_ID) {
          target.tasks.withType(DokkaTask::class.java).configureEach {
            configureDokkaForProject(it, target)
          }
          target.tasks.withType(DokkaTaskPartial::class.java).configureEach {
            configureDokkaPartial()
          }
        }
      }
    }
  }
}
