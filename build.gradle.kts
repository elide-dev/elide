@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.Properties

plugins {
  idea
  id("dev.elide.build")
  id("project-report")
  id("org.sonarqube")
  id("org.jetbrains.dokka")
  id("org.jetbrains.kotlinx.kover")
  id("org.jetbrains.kotlinx.binary-compatibility-validator")
  id("io.gitlab.arturbosch.detekt")

  id("com.android.application") version "7.3.1" apply false
  id("com.android.library") version "7.3.1" apply false

  alias(libs.plugins.qodana)
  alias(libs.plugins.ktlint)
  jacoco
  signing
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
val versionFile = File("./.version")
version = if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
  file(".version").readText().trim().replace("\n", "").ifBlank {
    throw IllegalStateException("Failed to load `.version`")
  }
} else if (project.hasProperty("version")) {
  project.properties["version"] as String
} else if (versionFile.exists()) {
  versionFile.readText()
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

val buildDocs by properties

buildscript {
  repositories {
    maven("https://maven.pkg.st/")
    gradlePluginPortal()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
  dependencies {
    classpath("org.jetbrains.kotlinx:kotlinx-knit:${libs.versions.kotlin.knit.get()}")
    classpath("com.guardsquare:proguard-gradle:${libs.versions.proguard.get()}")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:${libs.versions.dokka.get()}")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.sdk.get()}")
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
  // 16+ required for Apple Silicon support
  // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
  rootProject.the<NodeJsRootExtension>().download = true
  rootProject.the<NodeJsRootExtension>().nodeVersion = "18.11.0"
}
rootProject.plugins.withType(YarnPlugin::class.java) {
  rootProject.the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.WARNING
  rootProject.the<YarnRootExtension>().reportNewYarnLock = false
  rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

apiValidation {
  nonPublicMarkers += listOf(
    "elide.annotations.Internal",
  )

  ignoredProjects += listOf(
    "bundler",
    "bom",
    "cli",
    "proto",
    "processor",
    "reports",
  ).plus(
    if (project.properties["buildDocs"] == "true") {
      listOf("docs")
    } else {
      emptyList()
    }
  ).plus(
    if (project.properties["buildSamples"] == "true") {
      listOf("samples")
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
    property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/kover/merged/xml/report.xml")
  }
}

val dokkaVersion = libs.versions.dokka.get()
val mermaidDokka = libs.versions.mermaidDokka.get()

subprojects {
  val name = this.name

  apply {
    plugin("io.gitlab.arturbosch.detekt")
    plugin("org.jlleitschuh.gradle.ktlint")
    plugin("org.sonarqube")

    if (buildDocs == "true") {
      plugin("org.jetbrains.dokka")
    }
  }

  if (buildDocs == "true") {
    val dokkaPlugin by configurations
    dependencies {
      dokkaPlugin("org.jetbrains.dokka:versioning-plugin:$dokkaVersion")
      dokkaPlugin("org.jetbrains.dokka:templating-plugin:$dokkaVersion")
      dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")
    }
  }

  sonarqube {
    properties {
      if (!Elide.noTestModules.contains(name)) {
        when {
          // pure Java/Kotlin coverage
          Elide.serverModules.contains(name) -> {
            property("sonar.sources", "src/main/kotlin")
            property("sonar.tests", "src/test/kotlin")
            property("sonar.java.binaries", "$buildDir/classes/kotlin/main")
            property(
              "sonar.coverage.jacoco.xmlReportPaths",
              listOf(
                "$buildDir/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
                "$buildDir/reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml",
                "$buildDir/reports/jacoco/test/jacocoTestReport.xml",
                "$buildDir/reports/kover/xml/report.xml",
              )
            )
          }

          // KotlinJS coverage via Kover
          Elide.frontendModules.contains(name) -> {
            property("sonar.sources", "src/main/kotlin")
            property("sonar.tests", "src/test/kotlin")
            property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/kover/xml/report.xml")
          }

          // Kotlin MPP coverage via Kover
          Elide.multiplatformModules.contains(name) -> {
            property("sonar.sources", "src/commonMain/kotlin,src/jvmMain/kotlin,src/jsMain/kotlin,src/nativeMain/kotlin")
            property("sonar.tests", "src/commonTest/kotlin,src/jvmTest/kotlin,src/jsTest/kotlin,src/nativeTest/kotlin")
            property("sonar.java.binaries", "$buildDir/classes/kotlin/jvm/main")
            property(
              "sonar.coverage.jacoco.xmlReportPaths",
              listOf(
                "$buildDir/reports/kover/xml/report.xml",
              )
            )
          }
        }
      }
    }
  }

  ktlint {
    debug.set(false)
    verbose.set(false)
    android.set(false)
    outputToConsole.set(false)
    ignoreFailures.set(true)
    enableExperimentalRules.set(true)
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
    config = rootProject.files("config/detekt/detekt.yml")
  }

  val detektMerge by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.buildDir.resolve("reports/detekt/elide.sarif"))
  }

  plugins.withType(io.gitlab.arturbosch.detekt.DetektPlugin::class) {
    tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class) detekt@{
      finalizedBy(detektMerge)
      reports.sarif.required.set(true)
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

koverMerged {
  enable()

  xmlReport {
    onCheck.set(isCI)
  }

  htmlReport {
    onCheck.set(isCI)
  }
}

tasks.register("samples") {
  description = "Build and test all built-in code samples, in the `samples` path and with Knit."

  dependsOn(
    "buildSamples",
    "testSamples",
    "nativeTestSamples",
  )
}

tasks.register("buildSamples") {
  description = "Assemble all sample code."

  Elide.samplesList.forEach {
    dependsOn("$it:assemble")
  }
}

tasks.register("testSamples") {
  description = "Run all tests for sample code."

  Elide.samplesList.forEach {
    dependsOn("$it:test")
  }
}

tasks.register("nativeTestSamples") {
  description = "Run native (GraalVM) tests for sample code."

  Elide.samplesList.forEach {
    dependsOn("$it:nativeTest")
  }
}

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

afterEvaluate {
  tasks.named("koverMergedReport") {
    Elide.multiplatformModules.plus(
      Elide.serverModules
    ).plus(
      Elide.frontendModules
    ).forEach {
      dependsOn(":packages:$it:koverXmlReport")
    }
  }
}

if (buildDocs == "true") {
  tasks.named("dokkaHtmlMultiModule", DokkaMultiModuleTask::class).configure {
    includes.from("README.md")
    outputDirectory.set(buildDir.resolve("docs/kotlin/html"))
  }
}

tasks {
  htmlDependencyReport {
    projects = project.allprojects.filter {
      !Elide.multiplatformModules.contains(it.name)
    }.toSet()
  }

  htmlDependencyReport {
    reports.html.outputLocation.set(file("${project.buildDir}/reports/project/dependencies"))
  }
}

if (enableKnit == "true") {
  the<kotlinx.knit.KnitPluginExtension>().siteRoot = "https://beta.elide.dev/docs/kotlin"
  the<kotlinx.knit.KnitPluginExtension>().moduleDocs = "build/dokka/htmlMultiModule"
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

tasks.create("docs") {
  if (buildDocs == "true") {
    dependsOn(
      listOf(
        "dokkaHtml",
        "dokkaHtmlMultiModule",
        "dokkaJavadoc",
        "htmlDependencyReport",
        ":packages:server:dokkaJavadoc",
      )
    )
  }
}
