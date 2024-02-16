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
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.*
import java.util.Properties
import kotlinx.kover.gradle.plugin.dsl.*
import kotlinx.validation.KotlinApiCompareTask
import elide.internal.conventions.project.Projects

plugins {
  idea
  `project-report`
  alias(libs.plugins.kotlin.multiplatform) apply false

  alias(libs.plugins.cyclonedx)
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.detekt)
  alias(libs.plugins.dokka)
  alias(libs.plugins.gradle.checksum)
  alias(libs.plugins.gradle.testretry)
  alias(libs.plugins.kotlinx.plugin.abiValidator)
  alias(libs.plugins.kover)
  alias(libs.plugins.nexusPublishing)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.shadow)
  alias(libs.plugins.snyk)
  alias(libs.plugins.sonar)
  alias(libs.plugins.spdx.sbom)
  alias(libs.plugins.spotless)
  alias(libs.plugins.versionCatalogUpdate)

  id("elide.internal.conventions")
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
val versionFile: File = layout.projectDirectory.file(".version").asFile
version = if (hasProperty("elide.stamp") && properties["elide.stamp"] == "true") {
  versionFile.readText().trim().replace("\n", "").ifBlank {
    throw IllegalStateException("Failed to load `.version`")
  }
} else {
  "1.0-SNAPSHOT"
}

// Load property sources.
val props = Properties().apply {
  load(
    file(
      if (hasProperty("elide.ci") && properties["elide.ci"] == "true") {
        "gradle-ci.properties"
      } else {
        "local.properties"
      },
    ).inputStream(),
  )
}

val isCI = hasProperty("elide.ci") && properties["elide.ci"] == "true"

val javaLanguageVersion = properties["versions.java.language"] as String
val kotlinLanguageVersion = properties["versions.kotlin.language"] as String
val nodeVersion: String by properties
val enableKnit: String? by properties
val enableOwasp: String? by properties

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
    classpath(libs.kotlinx.knit)
    classpath(libs.plugin.kotlinx.atomicfu)
    classpath(libs.owasp)

    if (hasProperty("elide.pluginMode") && properties["elide.pluginMode"] == "repository") {
      classpath("dev.elide.buildtools:plugin:${properties["elide.pluginVersion"] as String}")
    }
  }
  if (findProperty("elide.lockDeps") == "true") {
    configurations.classpath {
      resolutionStrategy.activateDependencyLocking()
    }
  }
}

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
    val dokkaVersion: Provider<String> = libs.versions.dokka
    val mermaidDokka: Provider<String> = libs.versions.mermaidDokka
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:${dokkaVersion.get()}")
    dokkaPlugin("org.jetbrains.dokka:templating-plugin:${dokkaVersion.get()}")
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${dokkaVersion.get()}")
    dokkaPlugin("com.glureau:html-mermaid-dokka-plugin:${mermaidDokka.get()}")
  }
}

// --------------------------------------------------------------------------------------------------------------------
// EXTENSIONS
// --------------------------------------------------------------------------------------------------------------------

// --- IntelliJ IDEA --------------------------------------------------------------------------------------------------
//
idea {
  project {
    jdkName = (properties["elide.jvm"] as? String) ?: javaLanguageVersion
    languageLevel = IdeaLanguageLevel(javaLanguageVersion)
    vcs = "Git"
  }
}

// --- Sonatype -------------------------------------------------------------------------------------------------------
//
nexusPublishing {
  this@nexusPublishing.repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}

// --- OpenRewrite ----------------------------------------------------------------------------------------------------
//
rewrite {
  activeRecipe("org.openrewrite.java.OrderImports")
}

// --- Detekt ---------------------------------------------------------------------------------------------------------
//
detekt {
  parallel = true
  ignoreFailures = true
  config.from(files("config/detekt/detekt.yml"))
  baseline = file("config/detekt/baseline.xml")
  buildUponDefaultConfig = true
  enableCompilerPlugin = true
  basePath = projectDir.absolutePath
}

// --- Spotless -------------------------------------------------------------------------------------------------------
//
spotless {
  isEnforceCheck = false

  kotlinGradle {
    target("*.gradle.kts")
    diktat(libs.versions.diktat.get()).configFile(
      layout.projectDirectory.file("config/diktat/diktat.yml"),
    )
    ktlint(libs.versions.ktlint.get()).editorConfigOverride(
      mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
      ),
    )
  }
}

// --- Sonar ----------------------------------------------------------------------------------------------------------
//
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
      "sonar.kotlin.diktat.reportPaths" to "",
    ).filter { it.second.isNotBlank() }.forEach {
      property(it.first, it.second)
    }
  }
}

// --- Kover ----------------------------------------------------------------------------------------------------------
//
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

// --- API Pinning ----------------------------------------------------------------------------------------------------
//
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
      if (properties["buildDocs"] == "true") {
        listOf(
          "docs",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (properties["buildDocsSite"] == "true") {
        listOf(
          "site",
        )
      } else {
        emptyList()
      },
    )
}

// Conditional plugins to apply.
if (enableKnit == "true") apply(plugin = "kotlinx-knit")
if (enableOwasp == "true") apply(plugin = "org.owasp.dependencycheck")

// --- Node JS --------------------------------------------------------------------------------------------------------
//
plugins.withType(NodeJsRootPlugin::class.java) {
  val nodejs = the<NodeJsRootExtension>()
  nodejs.apply {
    download = true
  }

  the<NodeJsRootExtension>().version = nodeVersion
  if (nodeVersion.contains("canary")) {
    the<NodeJsRootExtension>().downloadBaseUrl = "https://nodejs.org/download/v8-canary"
  }
}

plugins.withType(YarnPlugin::class.java) {
  val yarn = the<YarnRootExtension>()
  yarn.apply {
    yarnLockMismatchReport = YarnLockMismatchReport.WARNING
    reportNewYarnLock = false
    yarnLockAutoReplace = false
    lockFileDirectory = rootDir
    lockFileName = "gradle-yarn.lock"
  }
}

// --- OWASP Dependency Check -----------------------------------------------------------------------------------------
//
configure<DependencyCheckExtension> {
  // Top-level settings
  format = listOf(HTML).joinToString(",") { it.toString() }
  scanBuildEnv = true
  scanDependencies = true
  autoUpdate = true

  // Caching
  cache.central = true
  cache.ossIndex = true
  cache.nodeAudit = true

  // NVD Settings
  nvd.apiKey = System.getenv("NVD_API_KEY")
  nvd.validForHours = 12

  // Analyzer settings
  suppressionFile = "config/owasp/owasp-suppressions.xml"
  scanConfigurations = listOf("compileClasspath", "runtimeClasspath", "classpath")

  analyzers.archiveEnabled = true
  analyzers.assemblyEnabled = false
  analyzers.bundleAuditEnabled = false
  analyzers.centralEnabled = true
  analyzers.jarEnabled = true
  analyzers.knownExploitedEnabled = true
  analyzers.msbuildEnabled = false
  analyzers.nodeEnabled = false
  analyzers.opensslEnabled = true

  analyzers.ossIndex.enabled = true
  analyzers.experimentalEnabled = false
}

// --- Snyk -----------------------------------------------------------------------------------------------------------
//
snyk {
  setArguments("--all-sub-projects")
  setSeverity("low")
  setAutoDownload(true)
  setAutoUpdate(true)
  System.getenv("SNYK_API_KEY")?.ifBlank { null }?.let {
    setApi(it)
  }
}

// --- Knit -----------------------------------------------------------------------------------------------------------
//
if (enableKnit == "true") {
  val knit = the<kotlinx.knit.KnitPluginExtension>()
  knit.apply {
    siteRoot = "https://docs.elide.dev/"
    moduleDocs = "docs/apidocs"
    files = fileTree(getRootDir()) {
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
  }
}

// --------------------------------------------------------------------------------------------------------------------
// TASKS
// --------------------------------------------------------------------------------------------------------------------

tasks {
  // --- Tasks: Kotlin/NPM
  //
  withType(KotlinNpmInstallTask::class.java).configureEach {
    packageJsonFiles.addFirst(layout.projectDirectory.file("package.json"))
    args.add("--ignore-engines")
    outputs.upToDateWhen {
      layout.projectDirectory.dir("node_modules").asFile.exists()
    }
  }

  withType(KotlinPackageJsonTask::class.java).configureEach {
    packageJson = file("package.json")
  }

  // --- Tasks: Detekt
  //
  val detektMergeSarif: TaskProvider<ReportMergeTask> = register("detektMergeSarif", ReportMergeTask::class.java) {
    output.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
  }
  val detektMergeXml: TaskProvider<ReportMergeTask> = register("detektMergeXml", ReportMergeTask::class.java) {
    output.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
  }
  withType(Detekt::class) detekt@{
    finalizedBy(detektMergeSarif, detektMergeXml)
    reports.sarif.required = true
    reports.xml.required = true
  }

  // --- Task: Resolve and Lock All Configurations
  //
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

  // --- Task: HTML Dependency Report
  //
  htmlDependencyReport {
    reports.html.outputLocation = layout.projectDirectory.dir("docs/reports").asFile
  }

  // --- Task: Reports
  //
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

  // --- Task: Knit
  //
  if (enableKnit == "true") {
    named("knitPrepare").configure {
      dependsOn("docs")
    }
  }

  // --- Task: Docs
  //
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

  // --- Task: Publish BOM
  //
  val publishBom by registering {
    description = "Publish BOM, Version Catalog, and platform artifacts to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(
      ":packages:bom:publishAllElidePublications",
      ":packages:platform:publishAllElidePublications",
    )
  }

  // --- Task: Publish Framework
  //
  val publishElide by registering {
    description = "Publish Elide library publications to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(
      Projects.publishedModules.map {
        project(it).tasks.named("publishAllElidePublications")
      },
    )
  }

  // --- Task: Publish Substrate
  //
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

  // --- Task: Publish All Targets
  //
  val publishAll by registering {
    description = "Publish all publications to Elide repositories and Maven Central"
    group = "Publishing"

    dependsOn(publishElide, publishSubstrate, publishBom)
  }

  // --- Task: Copy Coverage Reports
  //
  val copyCoverageReports by registering(Copy::class) {
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

  // --- Task: Quick-test
  //
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

  // --- Task: Pre-check
  //
  val precheck: TaskProvider<Task> by registering {
    description = "Run all pre-check tasks"
    group = "Verification"

    dependsOn(quicktest)
  }

  // --- Task: Pre-Merge
  //
  val preMerge by registering {
    description = "Runs all the tests/verification tasks"
    group = "Verification"

    dependsOn(
      detekt,
    )
  }

  // --- Task: Format
  //
  val format: TaskProvider<Task> by registering {
    description = "Run all formatting tasks"
    group = "Verification"

    val spotlessApply: Task by tasks
    dependsOn(spotlessApply)
  }

  // --- Task: Sonar
  //
  sonar.configure {
    dependsOn(
      detekt,
      koverBinaryReport,
      koverXmlReport,
      koverVerify,
    )
  }

  // --- Tasks: Kover Verification
  //
  koverVerify.configure {
    finalizedBy(copyCoverageReports)
  }

  // --- Tasks: Check
  //
  check.configure {
    dependsOn(
      spotlessCheck,
      koverVerify,
      preMerge,
      precheck,
      detekt,
      withType(KotlinApiCompareTask::class),
    )
  }
}
