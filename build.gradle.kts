@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import java.util.Properties

plugins {
  kotlin("plugin.allopen") version libs.versions.kotlin.sdk.get() apply false
  kotlin("plugin.serialization") version libs.versions.kotlin.sdk.get() apply false
  id("project-report")
  alias(libs.plugins.dokka)
  alias(libs.plugins.detekt)
  alias(libs.plugins.qodana)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.sonar)
  alias(libs.plugins.versionCheck)
  jacoco
  signing
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
version = if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
  file(".version").readText().trim().replace("\n", "").ifBlank {
    throw IllegalStateException("Failed to load `.version`")
  }
} else if (project.hasProperty("version")) {
  project.properties["version"] as String
} else {
  "1.0-SNAPSHOT"
}

val props = Properties()
props.load(file(if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
  "gradle-ci.properties"
} else {
  "local.properties"
}).inputStream())

val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String

tasks.dokkaHtmlMultiModule.configure {
  outputDirectory.set(buildDir.resolve("docs/kotlin/html"))
}

tasks.dokkaGfmMultiModule.configure {
  outputDirectory.set(buildDir.resolve("docs/kotlin/gfm"))
}

tasks.create("docs") {
  dependsOn(listOf(
    "dokkaHtmlMultiModule",
    "dokkaGfmMultiModule",
    ":packages:graalvm:dokkaJavadoc",
    ":packages:rpc-jvm:dokkaJavadoc",
    ":packages:server:dokkaJavadoc",
  ))
}

buildscript {
  repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.sdk.get()}")
    classpath("org.jetbrains.kotlinx:kotlinx-benchmark-plugin:${libs.versions.kotlinx.benchmark.plugin.get()}")
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

tasks.register("relock") {
  dependsOn(
    *(subprojects.map {
      it.tasks.named("dependencies")
    }.toTypedArray())
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
    property("sonar.jacoco.reportPath", "build/jacoco/test.exec")
    property("sonar.sourceEncoding", "UTF-8")
  }
}

subprojects {
  val name = this.name

  apply {
    plugin("io.gitlab.arturbosch.detekt")
    plugin("org.jlleitschuh.gradle.ktlint")
    plugin("org.sonarqube")
  }

  sonarqube {
    if (name != "base" && name != "test" && name != "model") {
      properties {
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.jacoco.reportPath", "build/jacoco/test.exec")
      }
    } else {
      properties {
        property("sonar.sources", "src/commonMain/kotlin,src/jvmMain/kotlin,src/jsMain/kotlin,src/nativeMain/kotlin")
        property("sonar.tests", "src/commonTest/kotlin,src/jvmTest/kotlin,src/jsTest/kotlin,src/nativeTest/kotlin")
      }
    }
  }

  ktlint {
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(true)
    enableExperimentalRules.set(true)
    filter {
      exclude("**/generated/**")
      exclude("**/tools/plugin/gradle-plugin/**")
      include("**/kotlin/**")
    }
  }

  detekt {
    ignoreFailures = true
    config = rootProject.files("config/detekt/detekt.yml")
  }

  afterEvaluate {
    if (tasks.findByName("check") != null) {
      tasks.getByName("check") {
        setDependsOn(dependsOn.filterNot {
          it is TaskProvider<*> && it.name == "detekt"
        })
      }

      tasks.getByName("build") {
        setDependsOn(dependsOn.filterNot {
          it is TaskProvider<*> && it.name == "check"
        })
      }
    }
  }

  if (project.property("elide.lockDeps") == "true") {
    dependencyLocking {
      lockAllConfigurations()
    }
  }
}

allprojects {
  repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    mavenCentral()
    google()
  }
  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = javaLanguageVersion
    targetCompatibility = javaLanguageVersion
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      jvmTarget = javaLanguageVersion
      javaParameters = true
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      target = ecmaVersion
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

tasks.register("reports") {
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
