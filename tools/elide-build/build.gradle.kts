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

@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  distribution
  signing
  idea
  java
  `jvm-toolchains`

  alias(libs.plugins.testLogger)
  alias(libs.plugins.versionCheck)

  id("java-gradle-plugin")
  `kotlin-dsl`
}

group = "dev.elide.tools"
version = rootProject.version as String

gradlePlugin {
  plugins {
    create("elideInternalBuild") {
      id = "elide.internal.conventions"
      implementationClass = "elide.internal.conventions.ElideConventionPlugin"
    }
    create("elideInternalCpp") {
      id = "elide.internal.cpp"
      implementationClass = "elide.internal.cpp.ElideCppPlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21

  toolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

kotlin {
  explicitApi()
  
  compilerOptions {
    jvmTarget = JVM_21
    javaParameters = true
    allWarningsAsErrors = false
    apiVersion = KOTLIN_2_0
    languageVersion = KOTLIN_2_0
    freeCompilerArgs = listOf(
      "-Xcontext-receivers",
      "-Xskip-prerelease-check",
      "-Xsuppress-version-warnings",
      "-Xjvm-default=all",
      "-Xjsr305=strict",
    )
  }
}

fun MutableList<String>.addIfNotPresent(arg: String) {
  if (!contains(arg)) add(arg)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = "2.0"
    languageVersion = "2.0"
    freeCompilerArgs = freeCompilerArgs.toMutableList().apply {
      addIfNotPresent("-Xcontext-receivers")
      addIfNotPresent("-Xskip-prerelease-check")
      addIfNotPresent("-Xsuppress-version-warnings")
      addIfNotPresent("-Xjvm-default=all")
      addIfNotPresent("-Xjsr305=strict")
    }
  }
}

dependencies {
  implementation(gradleApi())

  // included plugins
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.util)
  implementation(libs.guava)
  implementation(libs.h2)
  implementation(libs.jgit)
  implementation(libs.json)
  implementation(libs.kotlinpoet)
  implementation(libs.okio)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.dokka.base)
  implementation(libs.plugin.dokka.versioning)
  implementation(libs.plugin.dokka.templating)
  implementation(libs.plugin.dokka.kotlinAsJava)
  implementation(libs.plugin.dokka.mermaid)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.kotlin)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.ksp)
  implementation(libs.plugin.redacted)
  implementation(libs.plugin.sigstore)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)

  // embedded Kotlin plugins
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.kotlinx.atomicfu)
}

// Plugin: Test Logger
// -------------------
// Configure test logging.
testlogger {
  theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
  showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"
  showFailed = true
  showPassed = true
  showSkipped = true
  showFailedStandardStreams = true
  showFullStackTraces = true
  slowThreshold = 30000L
}

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
