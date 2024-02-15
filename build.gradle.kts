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

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.Properties
import kotlinx.kover.gradle.plugin.dsl.*
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
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.versionCatalogUpdate)
  alias(libs.plugins.gradle.checksum)
  alias(libs.plugins.spdx.sbom)
  alias(libs.plugins.cyclonedx)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)

  id("elide.internal.conventions")
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
val versionFile: File = rootProject.layout.projectDirectory.file(".version").asFile
version =
  if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
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
    },
  ).inputStream(),
)

val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val nodeVersion: String by properties
val enableKnit: String? by properties

val buildSamples: String by properties
val buildDocs: String by properties

buildscript {
  repositories {
    maven("https://maven.pkg.st")
    maven("https://gradle.pkg.st")

    maven {
      name = "elide-snapshots"
      url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
      }
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
      }
    }
  }
  dependencies {
    classpath("org.jetbrains.kotlinx:kotlinx-knit:${libs.versions.kotlin.knit.get()}")
    classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${libs.versions.atomicfu.get()}")

    if (project.hasProperty("elide.pluginMode") && project.properties["elide.pluginMode"] == "repository") {
      classpath("dev.elide.buildtools:plugin:${project.properties["elide.pluginVersion"] as String}")
    }
  }
  if (project.findProperty("elide.lockDeps") == "true") {
    configurations.classpath {
      resolutionStrategy.activateDependencyLocking()
    }
  }
}

apiValidation {
  nonPublicMarkers +=
    listOf(
      "elide.annotations.Internal",
    )

  ignoredProjects +=
    listOf(
      "bom",
      "cli",
      "embedded",
      "proto",
      "processor",
      "reports",
    ).plus(
      if (buildSamples == "true") {
        listOf(
          "samples",
          "basic",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (project.properties["buildDocs"] == "true") {
        listOf(
          "docs",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (project.properties["buildDocsSite"] == "true") {
        listOf(
          "site",
        )
      } else {
        emptyList()
      },
    )
}

idea {
  project {
    jdkName = (properties["elide.jvm"] as? String) ?: javaLanguageVersion
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

spotless {
  isEnforceCheck = false

  kotlinGradle {
    ktlint(libs.versions.ktlint.get()).apply {
      setEditorConfigPath(rootProject.layout.projectDirectory.file(".editorconfig"))
    }
  }
}

koverReport {
  defaults {
    filters {
      includes {
        packages("elide")
        classes("elide.*")
      }

      excludes {
        annotatedBy("*Generated*")

        packages(
          "tools.elide",
          "elide.cli",
          "elide.data",
          "elide.embedded.feature",
          "elide.proto",
          "elide.runtime.feature",
          "elide.tool",
          "elide.tools",
          "elide.vfs",
          "google",
          "kotlinx",
          "webutil",
        )
      }
    }

    xml {
      onCheck = false
    }

    html {
      onCheck = false
      title = "Elide Coverage"
    }

    binary {
      onCheck = true
    }

    verify {
      rule {
        isEnabled = true
        entity = GroupingEntityType.APPLICATION

        bound {
          minValue = 10
          maxValue = 99
          metric = MetricType.LINE
        }
        bound {
          minValue = 10
          maxValue = 99
          metric = MetricType.BRANCH
        }
        bound {
          minValue = 10
          maxValue = 99
          metric = MetricType.INSTRUCTION
        }
      }
    }
  }
}

detekt {
  parallel = true
  ignoreFailures = true
  config.from(rootProject.files("config/detekt/detekt.yml"))
  baseline = rootProject.file("config/detekt/baseline.xml")
  buildUponDefaultConfig = true
  enableCompilerPlugin = true
  basePath = projectDir.absolutePath
}

sonar {
  properties {
    property("sonar.projectKey", "elide-dev_v3")
    property("sonar.organization", "elide-dev")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.dynamicAnalysis", "reuseReports")
    property("sonar.junit.reportsPath", "build/reports/")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.sourceEncoding", "UTF-8")
    property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")

    listOf(
      "sonar.java.checkstyle.reportPaths" to "",
      "sonar.java.pmd.reportPaths" to "",
      "sonar.kotlin.detekt.reportPaths" to "build/reports/detekt/detekt.xml",
      "sonar.kotlin.ktlint.reportPaths" to "",
    ).filter { it.second.isNotBlank() }.forEach {
      property(it.first, it.second)
    }
  }
}

val dokkaVersion: Provider<String> = libs.versions.dokka
val mermaidDokka: Provider<String> = libs.versions.mermaidDokka

dependencies {
  // Kover: Coverage Reporting
  kover(projects.packages.base)
  kover(projects.packages.cli)
  kover(projects.packages.core)
  kover(projects.packages.embedded)
  kover(projects.packages.graalvm)
  kover(projects.packages.graalvmJava)
  kover(projects.packages.graalvmJs)
  kover(projects.packages.graalvmJvm)
  kover(projects.packages.graalvmKt)
  kover(projects.packages.graalvmLlvm)
  kover(projects.packages.graalvmPy)
  kover(projects.packages.graalvmRb)
  kover(projects.packages.http)
  kover(projects.packages.model)
  kover(projects.packages.proto.protoCore)
  kover(projects.packages.proto.protoKotlinx)
  kover(projects.packages.proto.protoProtobuf)
  kover(projects.packages.rpc)
  kover(projects.packages.runtime)
  kover(projects.packages.server)
  kover(projects.packages.serverless)
  kover(projects.packages.ssr)
  kover(projects.packages.test)
  kover(projects.packages.wasm)
  kover(projects.tools.processor)

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
  activeRecipe("org.openrewrite.java.OrderImports")
}

if (enableKnit == "true") apply(plugin = "kotlinx-knit")

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
  rootProject.the<NodeJsRootExtension>().apply {
    download = true
  }

  rootProject.the<NodeJsRootExtension>().version = nodeVersion
  if (nodeVersion.contains("canary")) {
    rootProject.the<NodeJsRootExtension>().downloadBaseUrl = "https://nodejs.org/download/v8-canary"
  }
}
rootProject.plugins.withType(YarnPlugin::class.java) {
  rootProject.the<YarnRootExtension>().apply {
    yarnLockMismatchReport = YarnLockMismatchReport.WARNING
    reportNewYarnLock = false
    yarnLockAutoReplace = false
    lockFileDirectory = project.rootDir
    lockFileName = "gradle-yarn.lock"
  }
}
tasks.withType(KotlinNpmInstallTask::class.java).configureEach {
  packageJsonFiles.addFirst(project.layout.projectDirectory.file("package.json"))
  args.add("--ignore-engines")
  outputs.upToDateWhen {
    project.rootProject.layout.projectDirectory.dir("node_modules").asFile.exists()
  }
}
tasks.withType(KotlinPackageJsonTask::class.java).configureEach {
  packageJson = project.rootProject.file("package.json")
}

tasks {
  val detektMergeSarif: TaskProvider<ReportMergeTask> = register("detektMergeSarif", ReportMergeTask::class.java) {
    output.set(project.rootProject.layout.buildDirectory.file("reports/detekt/detekt.sarif"))
  }
  val detektMergeXml: TaskProvider<ReportMergeTask> = register("detektMergeXml", ReportMergeTask::class.java) {
    output.set(project.rootProject.layout.buildDirectory.file("reports/detekt/detekt.xml"))
  }

  withType(Detekt::class) detekt@{
    finalizedBy(detektMergeSarif, detektMergeXml)
    reports.sarif.required = true
    reports.xml.required = true
  }

  val resolveAndLockAll by registering {
    doFirst {
      require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
      configurations.filter {
        // Add any custom filtering on the configurations to be resolved
        it.isCanBeResolved &&
                !it.name.lowercase().let { name ->
                  name.contains("sources") || name.contains("documentation")
                }
      }.forEach { it.resolve() }
    }
  }

  htmlDependencyReport {
    reports.html.outputLocation = layout.projectDirectory.dir("docs/reports").asFile
  }

  val reports by registering {
    description = "Build all reports."

    dependsOn(
      dependencyReport,
      htmlDependencyReport,
    )
  }

  if (buildDocs == "true") {
    val dokkaHtmlMultiModule by getting(DokkaMultiModuleTask::class) {
      moduleName = "Elide"
      includes.from(layout.projectDirectory.dir("docs/docs.md").asFile)
      outputDirectory = layout.projectDirectory.dir("docs/apidocs").asFile
    }
  }

  if (enableKnit == "true") {
    the<kotlinx.knit.KnitPluginExtension>().siteRoot = "https://docs.elide.dev/"
    the<kotlinx.knit.KnitPluginExtension>().moduleDocs = "docs/apidocs"
    the<kotlinx.knit.KnitPluginExtension>().files =
      fileTree(project.rootDir) {
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
    named("knitPrepare").configure {
      dependsOn("docs")
    }
  }

  val docs by registering {
    if (buildDocs == "true") {
      dependsOn(
        listOf(
          dokkaHtml,
          dokkaHtmlMultiModule,
          htmlDependencyReport,
        ),
      )
    }
  }

  val publishBom by registering {
    description = "Publish BOM, Version Catalog, and platform artifacts to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(
      ":packages:bom:publishAllElidePublications",
      ":packages:platform:publishAllElidePublications",
    )
  }

  val publishElide by registering {
    description = "Publish Elide library publications to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(
      Projects.publishedModules.map {
        project(it).tasks.named("publishAllElidePublications")
      },
    )
  }

  val publishSubstrate by registering {
    description = "Publish Elide Substrate and Kotlin compiler plugins to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(
      Projects.publishedSubprojects.map {
        gradle.includedBuild(it.substringBefore(":")).task(
          listOf(
            "",
            it.substringAfter(":"),
            "publishAllElidePublications",
          ).joinToString(":"),
        )
      },
    )
  }

  val publishAll by registering {
    description = "Publish all publications to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(publishElide, publishSubstrate, publishBom)
  }

  val copyCoverageReports by registering(Copy::class) {
    description = "Copy coverage reports to the root project for Qodana"
    group = "Verification"

    dependsOn(
      koverBinaryReport,
      koverXmlReport,
    )

    from(layout.buildDirectory.dir("reports/kover")) {
      include("report.bin", "report.xml", "verify.err")
      rename {
        it.replace("report.", "elide.")
      }
    }
    into(layout.projectDirectory.dir(".qodana/code-coverage"))
  }

  val quicktest: TaskProvider<Task> by registering {
    description = "Run all quick tests"
    group = "Verification"
    dependsOn(
      ":packages:core:jvmTest",
      ":packages:base:jvmTest",
      ":packages:graalvm:test",
      ":packages:serverless:test",
      ":packages:embedded:test",
    )
  }

  val precheck: TaskProvider<Task> by registering {
    description = "Run all pre-check tasks"
    group = "Verification"

    dependsOn(quicktest)
  }

  val preMerge by registering {
    description = "Runs all the tests/verification tasks"

    dependsOn(
      reports,
      detekt,
      check,
    )
  }

  afterEvaluate {
    precheck.configure {
      listOfNotNull(
        koverVerify,
        findByName("apiCheck"),
      ).forEach {
        dependsOn(it)
      }
    }
  }

  val format: TaskProvider<Task> by registering {
    description = "Run all formatting tasks"
    group = "Verification"

    val spotlessApply: Task by tasks
    dependsOn(spotlessApply)
  }

  check.configure {
    dependsOn(
      spotlessCheck,
      koverVerify,
      quicktest,
      precheck,
      detekt,
    )
  }

  sonar.configure {
    dependsOn(
      detekt,
      koverBinaryReport,
      koverXmlReport,
      koverVerify,
    )
  }

  koverVerify.configure {
    finalizedBy(copyCoverageReports)
  }
}

// @TODO: replace where needed with convention plugin logic
//
//subprojects {
//  val name = this.name
//
//  apply {
//    if (!Projects.nonKotlinProjects.contains(name)) {
//      if (buildDocs == "true" && !Projects.noDocModules.contains(name)) {
//        plugin("org.jetbrains.dokka")
//
//        val docAsset: (String) -> File = {
//          layout.projectDirectory.file("docs/$it").asFile
//        }
//        val creativeAsset: (String) -> File = {
//          layout.projectDirectory.file("creative/$it").asFile
//        }
//
//        tasks.withType<DokkaTask>().configureEach {
//          pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
//            footerMessage = "© 2022 Elide Ventures, LLC"
//            separateInheritedMembers = false
//            templatesDir = layout.projectDirectory.dir("docs/templates").asFile
//            customAssets =
//              listOf(
//                creativeAsset("logo/logo-wide-1200-w-r2.png"),
//                creativeAsset("logo/gray-elide-symbol-lg.png"),
//              )
//            customStyleSheets =
//              listOf(
//                docAsset(
//                  "styles/logo-styles.css",
//                ),
//              )
//          }
//        }
//      }
//    }
//  }
//}

// @TODO: replace where needed with convention plugin logic
//
//tasks.named<HtmlDependencyReportTask>("htmlDependencyReport") {
//  projects = project.allprojects
//}

// @TODO: replace where needed with convention plugin logic
//
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
