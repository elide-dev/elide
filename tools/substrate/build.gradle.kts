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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  `java-library`
  distribution
  signing
  idea
  kotlin("jvm")

  alias(libs.plugins.ktlint)
  alias(libs.plugins.dokka)
  alias(libs.plugins.versionCheck)
  alias(libs.plugins.testLogger)
  alias(libs.plugins.nexusPublishing)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
}

val allPlugins = listOf(
  "injekt",
  "interakt",
  "redakt",
  "sekret",
)

val enabledPlugins = listOf(
  "redakt",
)

group = "dev.elide.tools"
version = if (project.hasProperty("version")) {
  project.properties["version"] as String
} else {
  "1.0-SNAPSHOT"
}

tasks.create("buildPlugins") {
  description = "Build all Kotlin compiler plugins"
  dependsOn(enabledPlugins.map { ":$it:build" })
}

val libPlugins = libs.plugins
val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

tasks.named("build").configure {
  dependsOn("buildPlugins")
}

tasks.named("publish").configure {
  dependsOn(
    enabledPlugins.map {
      ":$it:publish"
    }.plus(
      listOf(
        ":bom:publish",
        ":compiler-util:publish",
      )
    )
  )
}

kotlin {
  explicitApi()
}

dependencies {
  kover(projects.compilerUtil)
  kover(projects.redakt)
}

extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverReportExtension> {
  defaults {
    xml {
      //  generate an XML report when running the `check` task
      onCheck = properties["elide.ci"] == "true"
    }
  }
}

sonarqube {
  properties {
    property("sonar.dynamicAnalysis", "reuseReports")
    property("sonar.junit.reportsPath", "build/reports/")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.sourceEncoding", "UTF-8")
    property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("reports/kover/merged/xml/report.xml"))
  }
}

subprojects {
  if (name != "bom") {
    sonarqube {
      properties {
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.java.binaries", layout.buildDirectory.dir("classes/kotlin/main"))
        property(
          "sonar.coverage.jacoco.xmlReportPaths",
          listOf(
            layout.buildDirectory.file("reports/kover/xml/report.xml"),
          )
        )
      }
    }
  }
}

dependencies {
  api(libs.elide.tools.compilerUtil)
  api(libs.elide.kotlin.plugin.redakt)
}

publishing {
  elideTarget(
    project,
    label = "Elide Tools: Substrate",
    group = project.group as String,
    artifact = "elide-substrate",
    summary = "BOM for Kotlin compiler plugins and other core project infrastructure.",
    parent = true,
  )
}

nexusPublishing {
  this@nexusPublishing.repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}
