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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.sonarqube.gradle.SonarExtension
import org.sonarqube.gradle.SonarQubePlugin
import elide.internal.conventions.ElideBuildExtension
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.kotlin.KotlinTarget.*

// All source set types.
private val sourceSetsMpp = listOf(
  "common",
  "js",
  "jvm",
  "native",
  "wasm",
  "nonJvm",
)

// All source sets for JVM.
private val sourceSetsJvm = listOf(
  "java",
  "kotlin",
  "groovy",
  "scala",
)

// All JVM main source set paths.
private val jvmSrcs = sourceSetsJvm.map {
  "src/main/$it"
}

// All JVM test source set paths.
private val jvmTestSrcs = sourceSetsJvm.map {
  "src/test/$it"
}

// All JVM main binary paths.
private val jvmBins = sourceSetsJvm.map {
  "build/classes/$it/main"
}

// Multiplatform sources; each directory is composed into `src/{type}Main/kotlin` form.
private val multiplatformSrcs = sourceSetsMpp.map {
  "src/${it}Main/kotlin"
}.plus(
  "src/jvmMain/java",   // also include Java sources
).sorted()

// Multiplatform test sources; each directory is composed into `src/{type}Test/kotlin` form.
private val multiplatformTestSrcs = sourceSetsMpp.map {
  "src/${it}Test/kotlin"
}.plus(
  "src/jvmTest/java",   // also include Java test sources
).sorted()

private fun List<String>.filterExistsInLayout(layout: Directory): String? {
  return filter { layout.dir(it).asFile.exists() }
    .joinToString(",")
    .ifBlank { null }
}

private fun SonarExtension.configureSonarForProject(conventions: ElideBuildExtension, project: Project) {
  // skip unrelated projects
  if (project.path.contains("crates")) {
    isSkipProject = true
    return
  }

  // cancel if skipped or non-kotlin/non java
  if ((!conventions.kotlin.requested && !conventions.java.requested) || !conventions.checks.sonar) return
  val target = conventions.kotlin.target
  val isJvm = target != null && JVM in target
  val isMultiplatform = target != null && (Native in target || KotlinTarget.Embedded == target)
  val isJavascript = target != null && (JsNode in target || JsBrowser in target)
  val projectPath = project.projectDir.absolutePath

  properties {
    property("sonar.verbose", "true")
    when {
      isMultiplatform -> {
        project.logger.lifecycle("Configuring Sonar Kotlin/MPP for project: ${project.name}")
        multiplatformSrcs.filterExistsInLayout(project.layout.projectDirectory)?.let { property("sonar.sources", it) }
        multiplatformTestSrcs.filterExistsInLayout(project.layout.projectDirectory)?.let { property("sonar.tests", it) }
        property("sonar.java.binaries", "build/classes/kotlin/jvm/main")
        property("sonar.junit.reportsPath", "build/test-results/jvmTest/")
        property("sonar.coverage.jacoco.xmlReportPaths", listOf(
          "$projectPath/build/reports/kover/report.xml",
          "$projectPath/build/reports/jacoco/test/jacocoTestReport.xml",
        ).joinToString(","))
      }

      isJavascript -> {
        project.logger.lifecycle("Configuring Sonar Kotlin/JS for project: ${project.name}")
        property("sonar.sources", "src/jsMain/kotlin")
        property("sonar.tests", "src/jsTest/kotlin")
        property("sonar.java.binaries", "build/classes/kotlin/js/main")
        property("sonar.coverage.jacoco.xmlReportPaths", "$projectPath/build/reports/kover/report.xml")
      }

      isJvm -> {
        project.logger.lifecycle("Configuring Sonar Kotlin/JVM for project: ${project.name}")
        jvmSrcs.filterExistsInLayout(project.layout.projectDirectory)?.let { property("sonar.sources", it) }
        jvmTestSrcs.filterExistsInLayout(project.layout.projectDirectory)?.let { property("sonar.tests", it) }

        (listOf(
          "classes/kotlin/main",
          "classes/java/main",
          "classes/kotlin/jvm/main",
        ).find {
          project.layout.buildDirectory.dir(it).get().asFile.exists()
        } ?: error("Failed to resolve `sonar.java.binaries` for project at '${project.path}'")).let { binPath ->
          property("sonar.java.binaries", "build/$binPath")
        }

        property("sonar.junit.reportsPath", "build/test-results/test/")
        property("sonar.coverage.jacoco.xmlReportPaths", listOf(
          "$projectPath/build/reports/kover/report.xml",
          "$projectPath/build/reports/kover/xml/coverage.xml",
          "$projectPath/build/reports/jacoco/test/jacocoTestReport.xml",
          "$projectPath/build/reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml",
          "$projectPath/build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
        ).joinToString(","))
      }

      else -> {
        isSkipProject = true
      }
    }
  }
}

public class SonarConventionsPlugin : Plugin<Project> {
  private companion object {
    const val SONAR_ID = "org.sonarqube"
  }

  override fun apply(target: Project) {
    val elide = target.extensions.getByType(ElideBuildExtension::class.java)
    if (elide.checks.sonar) {
      target.pluginManager.apply(SonarQubePlugin::class.java)
      target.pluginManager.withPlugin(SONAR_ID) {
        target.extensions.configure(SonarExtension::class.java) {
          configureSonarForProject(elide, target)
        }
      }
    }
  }
}
