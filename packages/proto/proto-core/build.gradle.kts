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
  "UNUSED_VARIABLE",
)

import ElidePackages.elidePackage

plugins {
  `maven-publish`
  distribution
  signing

  id("dev.elide.build.multiplatform.jvm")
  id("dev.elide.build.jvm11")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.minimum"] as String
val javaLanguageTarget = project.properties["versions.java.minimum"] as String

kotlin {
  jvm {
    withJava()
  }
  js()
  wasm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        // Common
        api(libs.kotlinx.datetime)
        implementation(kotlin("stdlib"))
        implementation(projects.packages.core)
        implementation(projects.packages.base)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(projects.packages.test)
      }
    }

    /**
     * Variant: Core
     */
    val jvmMain by getting {
      dependencies {
        // Common
        implementation(kotlin("stdlib-jdk8"))
      }
    }
    val jvmTest by getting {
      dependencies {
        // Common
        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        runtimeOnly(libs.junit.jupiter.engine)
      }
    }
  }
}

// Configurations: Testing
val testBase: Configuration by configurations.creating {}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
  options.compilerArgs.add("-Xlint:-deprecation")
}

tasks {
  jvmTest {
    useJUnitPlatform()
  }

  /**
   * Variant: Core
   */
  val testJar by registering(Jar::class) {
    description = "Base (abstract) test classes for all implementations"
    archiveClassifier = "tests"
    from(sourceSets.named("test").get().output)
  }

  artifacts {
    archives(jvmJar)
    add("testBase", testJar)
  }
}

elidePackage(
  id = "proto-core",
  name = "Elide Protocol: API",
  description = "API headers and services for the Elide Protocol.",
) {
  java9Modularity = false
}
