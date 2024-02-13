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

import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.Properties
import elide.internal.conventions.project.Projects

plugins {
  idea
  id("project-report")
  alias(libs.plugins.kotlin.multiplatform) apply false

  alias(libs.plugins.shadow)
  alias(libs.plugins.kotlinx.plugin.abiValidator)
  alias(libs.plugins.sonar)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
  alias(libs.plugins.nexusPublishing)
  alias(libs.plugins.gradle.testretry)
  alias(libs.plugins.buildTimeTracker)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.versionCatalogUpdate)
  alias(libs.plugins.gradle.checksum)
  alias(libs.plugins.spdx.sbom)
  alias(libs.plugins.cyclonedx)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.lombok)
  
  id("elide.internal.conventions")
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
val versionFile: File = rootProject.layout.projectDirectory.file(".version").asFile
version = if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
  versionFile.readText().trim().replace("\n", "").ifBlank {
    throw IllegalStateException("Failed to load `.version`")
  }
} else {
  "1.0-SNAPSHOT"
}

val props = Properties()
props.load(
  file(
    if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
      "gradle-ci.properties"
    } else {
      "local.properties"
    }
  ).inputStream()
)

val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String
val enableKnit: String? by properties
val enableProguard: String? by properties

val buildSsg: String by properties
val buildSamples: String by properties
val buildDocs: String by properties

buildscript {
  repositories {
    maven("https://maven.pkg.st")
    maven("https://gradle.pkg.st")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
  dependencies {
    val enableProguard: String? by properties
    classpath("org.jetbrains.kotlinx:kotlinx-knit:${libs.versions.kotlin.knit.get()}")
    if (enableProguard == "true") {
      classpath("com.guardsquare:proguard-gradle:${libs.versions.proguard.get()}")
    }
    if (project.hasProperty("elide.pluginMode") && project.properties["elide.pluginMode"] == "repository") {
      classpath("dev.elide.buildtools:plugin:${project.properties["elide.pluginVersion"] as String}")
    }
  }
  if (project.property("elide.lockDeps") == "true") {
    configurations.classpath {
      resolutionStrategy.activateDependencyLocking()
    }
  }
}

if (enableKnit == "true") apply(plugin = "kotlinx-knit")

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
  rootProject.the<NodeJsRootExtension>().download = true
  rootProject.the<NodeJsRootExtension>().version = "21.0.0-v8-canary20231024d0ddc81258"
  rootProject.the<NodeJsRootExtension>().downloadBaseUrl = "https://nodejs.org/download/v8-canary"
}
rootProject.plugins.withType(YarnPlugin::class.java) {
  rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING
  rootProject.the<YarnRootExtension>().reportNewYarnLock = false
  rootProject.the<YarnRootExtension>().yarnLockAutoReplace = false
  rootProject.the<YarnRootExtension>().lockFileDirectory = project.rootDir
  rootProject.the<YarnRootExtension>().lockFileName = "yarn.lock"
}
tasks.withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java).configureEach {
  args.add("--ignore-engines")
}

apiValidation {
  nonPublicMarkers += listOf(
    "elide.annotations.Internal",
  )

  ignoredProjects += listOf(
    "bom",
    "cli",
    "proto",
    "processor",
    "reports",
  ).plus(
    if (buildSamples == "true") {
      listOf(
        "samples",
        "basic",
      )
    } else emptyList()
  ).plus(
    if (buildSsg == "true") {
      listOf("bundler")
    } else {
      emptyList()
    }
  ).plus(
    if (project.properties["buildDocs"] == "true") {
      listOf(
        "docs",
      )
    } else {
      emptyList()
    }
  ).plus(
    if (project.properties["buildDocsSite"] == "true") {
      listOf(
        "site",
      )
    } else {
      emptyList()
    }
  )
}

tasks.register("relock") {
  dependsOn(
    *(
      subprojects.map {
          it.tasks.named("dependencies")
      }.toTypedArray()
    )
  )
}

sonarqube {
  properties {
    property("sonar.projectKey", "elide-dev_v3")
    property("sonar.organization", "elide-dev")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.dynamicAnalysis", "reuseReports")
    property("sonar.junit.reportsPath", "build/reports/")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.sourceEncoding", "UTF-8")
    property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("reports/kover/merged/xml/report.xml"))
  }
}

val dokkaVersion: Provider<String> = libs.versions.dokka
val mermaidDokka: Provider<String> = libs.versions.mermaidDokka

dependencies {
  // Kover: Merged Coverage Reporting
  kover(projects.packages.base)
  kover(projects.packages.cli)
  kover(projects.packages.core)
  kover(projects.packages.graalvm)
  kover(projects.packages.model)
  kover(projects.packages.proto.protoCore)
  kover(projects.packages.proto.protoCapnp)
  kover(projects.packages.proto.protoFlatbuffers)
  kover(projects.packages.proto.protoKotlinx)
  kover(projects.packages.proto.protoProtobuf)
  kover(projects.packages.rpc)
  kover(projects.packages.server)
  kover(projects.packages.ssr)
  kover(projects.packages.test)
  kover(projects.tools.processor)

  val buildSsg: String by properties
  if (buildSsg == "true") {
    kover(project(":packages:ssg"))
    kover(project(":tools:bundler"))
  }

  // OpenRewrite: Recipes
  rewrite(platform(libs.openrewrite.recipe.bom))

  if (buildDocs == "true") {
    val dokkaPlugin by configurations
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:${dokkaVersion.get()}")
    dokkaPlugin("org.jetbrains.dokka:templating-plugin:${dokkaVersion.get()}")
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${dokkaVersion.get()}")
    dokkaPlugin("com.glureau:html-mermaid-dokka-plugin:${mermaidDokka.get()}")
  }
}

rewrite {
  activeRecipe(
    "org.openrewrite.java.OrderImports",
  )
}

subprojects {
  val name = this.name

  apply {
    plugin("io.gitlab.arturbosch.detekt")
    plugin("org.jlleitschuh.gradle.ktlint")
    plugin("org.sonarqube")

    if (buildDocs == "true" && !Projects.noDocModules.contains(name)) {
      plugin("org.jetbrains.dokka")

      val docAsset: (String) -> File = {
        layout.projectDirectory.file("docs/$it").asFile
      }
      val creativeAsset: (String) -> File = {
        layout.projectDirectory.file("creative/$it").asFile
      }

      tasks.withType<DokkaTask>().configureEach {
        pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
          footerMessage = "© 2022 Elide Ventures, LLC"
          separateInheritedMembers = false
          templatesDir = layout.projectDirectory.dir("docs/templates").asFile
          customAssets = listOf(
            creativeAsset("logo/logo-wide-1200-w-r2.png"),
            creativeAsset("logo/gray-elide-symbol-lg.png"),
          )
          customStyleSheets = listOf(docAsset(
            "styles/logo-styles.css",
          ))
        }
      }
    }
  }

  sonarqube {
    properties {
      if (!Projects.noTestModules.contains(name)) {
        when {
          // pure Java/Kotlin coverage
          Projects.serverModules.contains(name) -> {
            property("sonar.sources", "src/main/kotlin")
            property("sonar.tests", "src/test/kotlin")
            property("sonar.java.binaries", layout.buildDirectory.dir("classes/kotlin/main"))
            property(
              "sonar.coverage.jacoco.xmlReportPaths",
              listOf(
                layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml"),
                layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml"),
                layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"),
                layout.buildDirectory.file("reports/kover/xml/coverage.xml"),
                layout.buildDirectory.file("reports/kover/xml/report.xml"),
              )
            )
          }

          // KotlinJS coverage via Kover
          Projects.frontendModules.contains(name) -> {
            property("sonar.sources", "src/main/kotlin")
            property("sonar.tests", "src/test/kotlin")
            property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("reports/kover/xml/report.xml"))
          }

          // Kotlin MPP coverage via Kover
          Projects.multiplatformModules.contains(name) -> {
            property("sonar.sources", "src/commonMain/kotlin,src/jvmMain/kotlin,src/jsMain/kotlin,src/nativeMain/kotlin")
            property("sonar.tests", "src/commonTest/kotlin,src/jvmTest/kotlin,src/jsTest/kotlin,src/nativeTest/kotlin")
            property("sonar.java.binaries", layout.buildDirectory.dir("classes/kotlin/jvm/main"))
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
  }

  ktlint {
    version = "0.50.0"

    debug = false
    verbose = false
    android = false
    outputToConsole = false
    ignoreFailures = true
    enableExperimentalRules = true
    coloredOutput = true

    filter {
      exclude("**/proto/**")
      exclude("**/generated/**")
      exclude("**/tools/plugin/gradle-plugin/**")
      include("**/kotlin/**")
    }
  }

  detekt {
    parallel = true
    ignoreFailures = true
    config.from(rootProject.files("config/detekt/detekt.yml"))
  }

  val detektMerge by tasks.registering(ReportMergeTask::class) {
    output = rootProject.layout.buildDirectory.file("reports/detekt/elide.sarif")
  }

  plugins.withType(io.gitlab.arturbosch.detekt.DetektPlugin::class) {
    tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class) detekt@{
      finalizedBy(detektMerge)
      reports.sarif.required = true
      detektMerge.configure {
        input.from(this@detekt.sarifReportFile) // or .sarifReportFile
      }
    }
  }

  afterEvaluate {
    if (tasks.findByName("check") != null) {
      tasks.getByName("check") {
        setDependsOn(
          dependsOn.filterNot {
            it is TaskProvider<*> && it.name == "detekt"
          }
        )
      }

      tasks.getByName("build") {
        setDependsOn(
          dependsOn.filterNot {
            it is TaskProvider<*> && it.name == "check"
          }
        )
      }
    }
  }

  if (project.property("elide.lockDeps") == "true") {
    dependencyLocking {
      lockAllConfigurations()
      lockMode = LockMode.LENIENT
    }
  }
}

tasks.register("resolveAndLockAll") {
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks)
  }
  doLast {
    configurations.filter {
      // Add any custom filtering on the configurations to be resolved
      it.isCanBeResolved
    }.forEach { it.resolve() }
  }
}

tasks.named<HtmlDependencyReportTask>("htmlDependencyReport") {
  projects = project.allprojects
}

if (tasks.findByName("resolveAllDependencies") == null) {
  tasks.register("resolveAllDependencies") {
    val npmInstall = tasks.findByName("kotlinNpmInstall")
    if (npmInstall != null) {
      dependsOn(npmInstall)
    }
    doLast {
      allprojects {
        configurations.forEach { c ->
          if (c.isCanBeResolved) {
            println("Downloading dependencies for '$path' - ${c.name}")
            val result = c.incoming.artifactView { lenient(true) }.artifacts
            result.failures.forEach {
              println("- Ignoring Error: ${it.message}")
            }
          }
        }
      }
    }
  }
}

/*tasks.register("samples") {
  description = "Build and test all built-in code samples, in the `samples` path and with Knit."

  //dependsOn(
    //"buildSamples",
    //"testSamples",
    //"nativeTestSamples",
  //)
}

tasks.register("buildSamples") {
  description = "Assemble all sample code."

  Projects.samples.forEach {
    dependsOn("$it:assemble")
  }
}

tasks.register("testSamples") {
  description = "Run all tests for sample code."

  Projects.samples.forEach {
    dependsOn("$it:test")
  }
}

tasks.register("nativeTestSamples") {
  description = "Run native (GraalVM) tests for sample code."

  Projects.samples.forEach {
    dependsOn("$it:nativeTest")
  }
}*/

tasks.register("reports") {
  description = "Build all reports."

  dependsOn(
    ":dependencyReport",
    ":htmlDependencyReport",
  )
}

tasks.register("preMerge") {
  description = "Runs all the tests/verification tasks"

  dependsOn(
    ":reports",
    ":detekt",
    ":ktlintCheck",
    ":check",
  )
}

if (buildDocs == "true") {
  tasks.named("dokkaHtmlMultiModule", DokkaMultiModuleTask::class).configure {
    moduleName = "Elide"
    includes.from(layout.projectDirectory.dir("docs/docs.md").asFile)
    outputDirectory = layout.projectDirectory.dir("docs/apidocs").asFile
  }
}

tasks {
  htmlDependencyReport {
    projects = project.allprojects.filter {
      !Projects.multiplatformModules.contains(it.name)
    }.toSet()
  }

  htmlDependencyReport {
    reports.html.outputLocation = layout.projectDirectory.dir("docs/reports").asFile
  }
}

if (enableKnit == "true") {
  the<kotlinx.knit.KnitPluginExtension>().siteRoot = "https://docs.elide.dev/"
  the<kotlinx.knit.KnitPluginExtension>().moduleDocs = "docs/apidocs"
  the<kotlinx.knit.KnitPluginExtension>().files = fileTree(project.rootDir) {
    include("README.md")
    include("docs/guide/**/*.md")
    include("docs/guide/**/*.kt")
    include("samples/**/*.md")
    include("samples/**/*.kt")
    include("samples/**/*.kts")
    exclude("**/build/**")
    exclude("**/.gradle/**")
    exclude("**/node_modules/**")
  }

  // Build API docs via Dokka before running Knit.
  tasks.named("knitPrepare").configure {
    dependsOn("docs")
  }
}

val jvmName = project.properties["elide.jvm"] as? String

idea {
  project {
    jdkName = jvmName ?: javaLanguageVersion
    languageLevel = IdeaLanguageLevel(javaLanguageVersion)
    vcs = "Git"
  }
}

nexusPublishing {
  this@nexusPublishing.repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}

tasks.create("docs") {
  if (buildDocs == "true") {
    dependsOn(
      listOf(
        "dokkaHtml",
        "dokkaHtmlMultiModule",
        "htmlDependencyReport",
      )
    )
  }
}

val publishBom by tasks.registering {
  description = "Publish BOM, Version Catalog, and platform artifacts to Elide repositories and Maven Central"
  group = "Publishing"

  dependsOn(
    ":packages:bom:publishAllElidePublications",
    ":packages:platform:publishAllElidePublications",
  )
}

val publishElide by tasks.registering {
  description = "Publish Elide library publications to Elide repositories and Maven Central"
  group = "Publishing"

  dependsOn(Projects.publishedModules.map {
    project(it).tasks.named("publishAllElidePublications")
  })
}

val publishSubstrate by tasks.registering {
  description = "Publish Elide Substrate and Kotlin compiler plugins to Elide repositories and Maven Central"
  group = "Publishing"

  dependsOn(Projects.publishedSubprojects.map {
    gradle.includedBuild(it.substringBefore(":")).task(listOf(
      "",
      it.substringAfter(":"),
      "publishAllElidePublications"
    ).joinToString(":"))
  })
}

val publishAll by tasks.registering {
  description = "Publish all publications to Elide repositories and Maven Central"
  group = "Publishing"

  dependsOn(publishElide, publishSubstrate, publishBom)
}
