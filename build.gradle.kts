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

/**
 * Elide Runtime + Framework
 */

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.SARIF
import java.util.Properties
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType
import kotlinx.kover.gradle.plugin.dsl.MetricType
import kotlinx.validation.KotlinApiCompareTask
import elide.internal.conventions.project.Projects

plugins {
  idea
  java
  `jvm-toolchains`

  alias(libs.plugins.gradle.checksum)
  alias(libs.plugins.gradle.testretry)
  alias(libs.plugins.kotlinx.plugin.abiValidator)
  alias(libs.plugins.nexusPublishing)
  alias(libs.plugins.spdx.sbom)

  id(libs.plugins.detekt.get().pluginId)
  id(libs.plugins.dokka.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.spotless.get().pluginId)

  alias(libs.plugins.elide.conventions)
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
  if (hasProperty("elide.ci") && properties["elide.ci"] == "true") {
    if (layout.projectDirectory.file("gradle-ci.properties").asFile.exists()) {
      load(file("gradle-ci.properties").inputStream())
    }
  } else {
    if (layout.projectDirectory.file("local.properties").asFile.exists()) {
      load(file("local.properties").inputStream())
    }
  }
}

val isCI = hasProperty("elide.ci") && properties["elide.ci"] == "true"

val javaLanguageVersion = properties["versions.java.language"] as String
val kotlinLanguageVersion = properties["versions.kotlin.language"] as String
val nodeVersion: String by properties
val enableOwasp: String? by properties

val buildSamples: String by properties
val buildDocs: String by properties
val buildDeprecated: String by properties
val buildEmbedded: String by properties
val buildAuxImage: String by properties
val buildDocsModules: String by properties

buildscript {
  repositories {
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
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
    mavenCentral()
    gradlePluginPortal()
  }

  dependencies {
    classpath(libs.bouncycastle)
    classpath(libs.bouncycastle.util)
    classpath(libs.guava)
    classpath(libs.h2)
    classpath(libs.jgit)
    classpath(libs.jgit)
    classpath(libs.json)
    classpath(libs.kotlinpoet)
    classpath(libs.okio)
    classpath(libs.owasp)
    classpath(libs.plugin.kotlinx.atomicfu)
    classpath(libs.protobuf.java)
    classpath(libs.protobuf.util)

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

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

dependencies {
  // Kover: Coverage Reporting
  kover(projects.packages.base)
  kover(projects.packages.cli)
  kover(projects.packages.core)
  kover(projects.packages.graalvm)
  kover(projects.packages.graalvmJava)
  kover(projects.packages.graalvmJvm)
  kover(projects.packages.graalvmKt)
  kover(projects.packages.graalvmLlvm)
  kover(projects.packages.graalvmPy)
  kover(projects.packages.graalvmRb)
  kover(projects.packages.http)
  kover(projects.packages.proto.protoCore)
  kover(projects.packages.proto.protoKotlinx)
  kover(projects.packages.proto.protoProtobuf)
  kover(projects.packages.server)
  kover(projects.packages.ssr)
  kover(projects.packages.test)

  if (buildDeprecated == "true") {
    kover(project(":packages:wasm"))
    kover(project(":packages:serverless"))
    kover(project(":packages:embedded"))
    kover(project(":tools:processor"))
  }

  if (buildDocs == "true") {
    val dokkaPlugin by configurations
    dokkaPlugin(libs.plugin.dokka.versioning)
    dokkaPlugin(libs.plugin.dokka.templating)
    dokkaPlugin(libs.plugin.dokka.kotlinAsJava)
    dokkaPlugin(libs.plugin.dokka.mermaid)
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
    property("sonar.verbose", "true")
    property("sonar.projectKey", "elide-dev_v3")
    property("sonar.organization", "elide-dev")
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.dynamicAnalysis", "reuseReports")
    property("sonar.junit.reportsPath", "build/reports/")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.java.enablePreview", "true")
    property("sonar.sourceEncoding", "UTF-8")
    property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")

    listOf(
      "sonar.java.checkstyle.reportPaths" to "build/reports/checkstyle/main.xml",
      "sonar.java.pmd.reportPaths" to "build/reports/pmd/main.xml",
      "sonar.sarifReportPaths" to "reports/detekt/detekt.sarif",
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
      "cli",
      "sqlite",
      "transport",
      "transport-common",
      "transport-kqueue",
      "transport-epoll",
      "transport-uring",
      "proto",
      "reports",
    ).plus(
      if (buildAuxImage == "true") {
        listOf(
          "auximage",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (buildDocs == "true" && buildDocsModules == "true") {
        listOf(
          "architecture",
          "docs",
          "guide",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (buildDeprecated == "true") {
        listOf(
          "bom",
          "processor",
        )
      } else {
        emptyList()
      },
    ).plus(
      if (buildEmbedded == "true") {
        listOf(
          "embedded",
        )
      } else {
        emptyList()
      },
    )
}

// Conditional plugins to apply.
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
  format = listOf(HTML, SARIF).joinToString(",") { it.toString() }
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

// --------------------------------------------------------------------------------------------------------------------
// TASKS
// --------------------------------------------------------------------------------------------------------------------

tasks {
  // --- Tasks: Kotlin/NPM
  //
  withType(KotlinNpmInstallTask::class.java).configureEach {
    (packageJsonFiles as MutableList<RegularFile>).addFirst(layout.projectDirectory.file("package.json"))
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
  val detektMergeSarif: TaskProvider<ReportMergeTask> by registering(ReportMergeTask::class) {
    output.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
  }
  val detektMergeXml: TaskProvider<ReportMergeTask> by registering(ReportMergeTask::class) {
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

  if (buildDocs == "true") {
    val docAsset: (String) -> File = {
      rootProject.layout.projectDirectory.file("docs/$it").asFile
    }
    val creativeAsset: (String) -> File = {
      rootProject.layout.projectDirectory.file("creative/$it").asFile
    }

    val dokkaHtmlMultiModule by getting(DokkaMultiModuleTask::class) {
      moduleName = "Elide API"
      moduleVersion = project.version as String
      outputDirectory = layout.projectDirectory.dir("docs/apidocs").asFile

      suppressInheritedMembers = true
      suppressObviousFunctions = true

      includes.from(
        listOf(
          "docs/docs.md",
          "docs/includes/resources.md",
        ).map {
          layout.projectDirectory.file(it).asFile
        },
      )

      pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        footerMessage = "© 2023—2024 Elide Technologies, Inc."
        homepageLink = "https://docs.elide.dev"
        templatesDir = rootProject.layout.projectDirectory.dir("docs/templates").asFile
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
        versionsOrdering = listOf("1.0.0-alpha8")
        olderVersionsDir = file("docs/versions")
        olderVersions = listOf(file("docs/versions/1.0.0-alpha8"))
        renderVersionsNavigationOnAllPages = true
      }
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
    mustRunAfter(
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
