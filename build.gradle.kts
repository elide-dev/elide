@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

import java.util.Properties

plugins {
  kotlin("plugin.serialization") version "1.7.20" apply false
  id("com.google.cloud.artifactregistry.gradle-plugin")
  id("org.jetbrains.dokka") version "1.7.0"
  id("org.jetbrains.kotlinx.kover") version "0.5.1"
  id("org.sonarqube") version "3.4.0.2513"
}

group = "dev.elide"

// Set version from `.version` if stamping is enabled.
version = if (project.hasProperty("elide.stamp") && project.properties["elide.stamp"] == "true") {
  file(".version").readText().trim().replace("\n", "")
} else {
  "1.0-SNAPSHOT"
}

val props = Properties()
props.load(file(if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
  "gradle-ci.properties"
} else {
  "local.properties"
}).inputStream())

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
  ))
}

buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    maven("https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("com.bmuschko:gradle-docker-plugin:${Versions.dockerPlugin}")
    classpath("com.github.node-gradle:gradle-node-plugin:${Versions.nodePlugin}")
    classpath("gradle.plugin.com.google.cloud.artifactregistry:artifactregistry-gradle-plugin:${Versions.gauthPlugin}")
    classpath("io.micronaut.gradle:micronaut-gradle-plugin:${Versions.micronautPlugin}")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:${Versions.kotlin}")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    classpath("org.jetbrains.kotlinx:kover:${Versions.koverPlugin}")
    classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.atomicfuPlugin}")
    classpath("com.adarshr:gradle-test-logger-plugin:${Versions.testLoggerPlugin}")
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

if (project.property("elide.lockDeps") == "true") {
  subprojects {
    dependencyLocking {
      lockAllConfigurations()
    }
  }
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
  }
}

subprojects {
  val name = this.name

  sonarqube {
    if (name != "base") {
      properties {
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property(
          "sonar.coverage.jacoco.xmlReportPaths",
          listOf(
            "build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml",
            "build/reports/jacoco/testCodeCoverageReport/jacocoTestReport.xml",
            "build/reports/jacoco/test/jacocoTestReport.xml",
            "build/reports/kover/xml/coverage.xml",
          )
        )
      }
    } else {
      properties {
        property("sonar.sources", "src/commonMain/kotlin,src/jvmMain/kotlin,src/jsMain/kotlin,src/nativeMain/kotlin")
        property("sonar.tests", "src/commonTest/kotlin,src/jvmTest/kotlin,src/jsTest/kotlin,src/nativeTest/kotlin")
      }
    }
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    jcenter()
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
      jvmTarget = Versions.javaLanguage
      javaParameters = true
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
}

kover {
  if (project.hasProperty("elide.ci") && (project.properties["elide.ci"] as String) == "true") {
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.JACOCO)
  } else {
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ)
  }
}

tasks.koverMergedHtmlReport {
  isEnabled = true
  htmlReportDir.set(layout.buildDirectory.dir("${rootProject.buildDir}/reports/kover/html"))
}

tasks.koverMergedXmlReport {
  isEnabled = true
  xmlReportFile.set(layout.buildDirectory.file("${rootProject.buildDir}/reports/kover/xml/coverage.xml"))
}

subprojects {
  tasks.koverXmlReport {
    isEnabled = true
    xmlReportFile.set(layout.buildDirectory.file("${project.buildDir}/reports/kover/xml/coverage.xml"))
  }
}
