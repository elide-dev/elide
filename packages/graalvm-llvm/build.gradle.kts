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

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "graalvm-llvm"
    name = "Elide LLVM for GraalVM"
    description = "Integration package with GraalVM, and LLVM."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  java {
    // disable module-info processing (not present)
    configureModularity = false
  }

  native {
    target = NativeTarget.LIB
  }
}

val oracleGvm = true

dependencies {
  api(libs.bundles.graalvm.llvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(projects.packages.engine)
  compileOnly(libs.graalvm.llvm.language.nfi)
  compileOnly(libs.graalvm.llvm.language.native.resources)

  if (oracleGvm) {
    compileOnly(libs.graalvm.llvm.language.managed)
    compileOnly(libs.graalvm.llvm.language.enterprise)
    compileOnly(libs.graalvm.llvm.language.native.enterprise)
  }

  compileOnly(libs.graalvm.svm)
  if (oracleGvm) {
    compileOnly(libs.graalvm.truffle.enterprise)
  }

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(projects.packages.graalvm)
  testImplementation(project(":packages:graalvm", configuration = "testBase"))
}

tasks.jar.configure {
  exclude("**/runtime.current.json")
}
