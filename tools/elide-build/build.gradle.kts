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

@file:Suppress("UnstableApiUsage", "MagicNumber")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  distribution
  signing
  idea
  java
  `jvm-toolchains`

  // Temporarily disabled due to dependency resolution issues with Gradle 8.11.1
  // alias(libs.plugins.testLogger)
  // alias(libs.plugins.versionCheck)

  id("java-gradle-plugin")
  `kotlin-dsl`
}

group = "dev.elide.tools"
version = rootProject.version as String

repositories {
  maven {
    name = "oss-snapshots"
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    content {
      includeGroup("com.google.devtools.ksp")
      includeGroup("com.google.devtools.ksp.gradle.plugin")
    }
  }
  maven {
    name = "maven-central-explicit"
    url = uri("https://repo1.maven.org/maven2/")
  }
  gradlePluginPortal()
  mavenCentral()
  google()
}

gradlePlugin {
  plugins {
    create("elideInternalBuild") {
      id = "elide.internal.conventions"
      implementationClass = "elide.internal.conventions.ElideConventionPlugin"
    }
    create("elideToolchainManager") {
      id = "elide.toolchains.jvm"
      implementationClass = "elide.toolchain.jvm.JvmToolchainResolverPlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_22
  targetCompatibility = JavaVersion.VERSION_22

  toolchain {
    languageVersion = JavaLanguageVersion.of(24)
  }
}

kotlin {
  explicitApi()
  
  compilerOptions {
    jvmTarget = JVM_22
    javaParameters = true
    allWarningsAsErrors = true
    apiVersion = KOTLIN_2_1
    languageVersion = KOTLIN_2_1
    freeCompilerArgs = listOf(
      "-Xcontext-receivers",
      "-Xskip-prerelease-check",
      "-Xsuppress-version-warnings",
      "-Xjsr305=strict",
    )
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget = JVM_22
    apiVersion = KOTLIN_2_1
    languageVersion = KOTLIN_2_1
  }
}

dependencies {
  implementation(gradleApi())
  api(libs.asm.core)
  api(libs.asm.tree)

  // included plugins and dependencies
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.util)
  implementation(libs.guava)
  implementation(libs.h2)
  implementation(libs.jgit)
  implementation(libs.json)
  implementation(libs.okio)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.dokka.base)
  implementation(libs.plugin.dokka.versioning)
  implementation(libs.plugin.dokka.templating)
  implementation(libs.plugin.dokka.mermaid)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.kotlin)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.ksp)
  implementation(libs.plugin.shadow)
  implementation(libs.plugin.sigstore)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)

  // jetbrains
  implementation(libs.plugin.compose)

  // embedded Kotlin plugins
  implementation(libs.kotlinx.metadata.jvm)
  implementation(libs.plugin.kotlin.atomicfu)
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlin.powerAssert)
  implementation(libs.plugin.kotlin.compose)
  implementation(libs.plugin.kotlin.jsObjects)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.kotlinx.atomicfu)
  implementation(libs.plugin.kotlinx.abiValidator) {
    exclude(group = "org.objectweb.asm", module = "asm")
    exclude(group = "org.objectweb.asm", module = "asm-tree")
  }
}

// Plugin: Test Logger
// -------------------
// Configure test logging.
// Temporarily commented out - see gradle-kotlin-analysis.md
/*
testlogger {
  theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
  showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"
  showFailed = true
  showPassed = true
  showSkipped = true
  showFailedStandardStreams = true
  showStandardStreams = true
  showFullStackTraces = true
  showPassedStandardStreams = false
  showSkippedStandardStreams = false
  slowThreshold = 30000L
}
*/

// Tasks: Test
// -----------
// Settings for testsuite execution and test retries.
tasks.withType<Test>().configureEach {
  maxParallelForks = 4
}

// Tasks: Tar
// ----------
// Configure tasks which produce tarballs (improves caching/hermeticity).
tasks.withType<Jar>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
  isZip64 = true
}

// Tasks: Zip
// ----------
// Configure tasks which produce zip archives (improves caching/hermeticity).
tasks.withType<Zip>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
  isZip64 = true
}


// Dependencies: Locking
// ---------------------
// Produces sealed dependency locks for each module.
dependencyLocking {
  lockMode = LockMode.LENIENT
  ignoredDependencies.addAll(listOf(
    "org.jetbrains.kotlinx:atomicfu*",
    "org.jetbrains.kotlinx:kotlinx-serialization*",
  ))
}

// Dependencies: Conflicts
// -----------------------
// Establishes a strict conflict policy for dependencies.

val lockedConfigs = listOf(
  "classpath",
  "compileClasspath",
  "runtimeClasspath",
)

configurations.all {
  resolutionStrategy {
    // fail eagerly on version conflict (includes transitive dependencies)
    failOnVersionConflict()

    // prefer modules that are part of this build
    preferProjectModules()

    if (lockedConfigs.contains(name) && project.findProperty("elide.lockDeps") == "true") {
      // lock by default
      activateDependencyLocking()
    }
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    val asm = libs.asm.core.get()
    if (requested.group == asm.group && requested.name == asm.name) {
      useVersion(libs.versions.asm.get())
      because("need better bytecode support")
    }

    // Force Kotlin dependencies to use the correct version
    if (requested.group == "org.jetbrains.kotlin") {
      if (requested.version == "2.2.20" || requested.version == "2.0.21") {
        useVersion(libs.versions.kotlin.sdk.get())
        because("version ${requested.version} doesn't exist or isn't accessible, using ${libs.versions.kotlin.sdk.get()}")
      }
    }
  }
}
