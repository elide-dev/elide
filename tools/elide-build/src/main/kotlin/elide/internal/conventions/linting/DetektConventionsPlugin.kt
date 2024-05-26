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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.withType
import elide.internal.conventions.ElideBuildExtension

// Root-relative path to the Detekt configuration file.
private const val detektConfig = "config/detekt/detekt.yml"

// Root-relative path to the main Detekt report file, in XML format.
private const val mergedReportXml = "reports/detekt/detekt.xml"

// Root-relative path to the main Detekt report file, in SARIF format.
private const val mergedReportSarif = "reports/detekt/detekt.sarif"

private fun DetektExtension.configureDetektForProject(conventions: ElideBuildExtension, project: Project) {
  parallel = true
  ignoreFailures = conventions.checks.ignoreFailures
  config.from(project.rootProject.files(detektConfig))
  buildUponDefaultConfig = true
  basePath = project.rootProject.projectDir.absolutePath
  enableCompilerPlugin.set(true)

  val detektMergeSarif = project.tasks.register("detektMergeSarif", ReportMergeTask::class.java) {
    output.set(project.rootProject.layout.buildDirectory.file(mergedReportSarif))
  }
  val detektMergeXml = project.tasks.register("detektMergeXml", ReportMergeTask::class.java) {
    output.set(project.rootProject.layout.buildDirectory.file(mergedReportXml))
  }

  project.tasks.withType(Detekt::class) detekt@{
    finalizedBy(detektMergeSarif, detektMergeXml)
    reports.sarif.required = true
    reports.xml.required = true
    reports.sarif.outputLocation.set(project.layout.buildDirectory.file("reports/detekt/detekt.sarif"))
    reports.xml.outputLocation.set(project.layout.buildDirectory.file("reports/detekt/detekt.xml"))
    jvmTarget = if (conventions.jvm.forceJvm17) "17" else "21"  // @TODO pull from property state

    detektMergeSarif.configure {
      input.from(this@detekt.sarifReportFile)
    }
    detektMergeXml.configure {
      input.from(this@detekt.xmlReportFile)
    }
  }
  project.tasks.withType(DetektCreateBaselineTask::class) detekt@{
    jvmTarget = if (conventions.jvm.forceJvm17) "17" else "21"  // @TODO pull from property state
  }
}

public class DetektConventionsPlugin : Plugin<Project> {
  private companion object {
    const val DETEKT_ID = "io.gitlab.arturbosch.detekt"
  }

  override fun apply(target: Project) {
    val elide = target.extensions.getByType(ElideBuildExtension::class.java)
    if (elide.checks.detekt) {
      target.pluginManager.apply(DetektPlugin::class.java)
      target.pluginManager.withPlugin(DETEKT_ID) {
        target.extensions.configure(DetektExtension::class.java) {
          configureDetektForProject(elide, target)
        }
      }
    } else {
      if (target.pluginManager.hasPlugin(DETEKT_ID)) {
        target.tasks.withType(Detekt::class) {
          enabled = false
          jvmTarget = "21"  // @TODO pull from property state
        }
        target.tasks.withType(DetektCreateBaselineTask::class) {
          enabled = false
          jvmTarget = "21"  // @TODO pull from property state
        }
      }
    }
  }
}
