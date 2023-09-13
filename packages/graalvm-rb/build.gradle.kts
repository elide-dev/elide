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

import ElidePackages.elidePackage

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("kapt")
  kotlin("plugin.allopen")

  id("dev.elide.build.native.lib")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val encloseSdk = !System.getProperty("java.vm.version").contains("jvmci")

kotlin {
  explicitApi()
}

graalvmNative.agent.enabled = false

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmLlvm)

  if (encloseSdk) {
    compileOnly(libs.graalvm.sdk)
    compileOnly(libs.graalvm.truffle.api)
    testCompileOnly(libs.graalvm.sdk)
    testCompileOnly(libs.graalvm.truffle.api)
  }

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

tasks {
  jar.configure {
    exclude("**/runtime.current.json")
  }

  test {
    enabled = false  // @TODO(sgammon): temporary while broken

    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")

    javaToolchains {
      javaLauncher.set(launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.GRAAL_VM
      })
    }
  }
}

elidePackage(
  id = "graalvm-rb",
  name = "Elide Ruby for GraalVM",
  description = "Integration package with GraalVM and TruffleRuby.",
) {
  java9Modularity = false
}
