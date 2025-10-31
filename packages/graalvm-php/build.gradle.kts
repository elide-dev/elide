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
    id = "graalvm-php"
    name = "Elide PHP for GraalVM"
    description = "Integration package with GraalVM, and PHP."

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
  annotationProcessor(libs.graalvm.truffle.processor)
  api(libs.graalvm.truffle.api)
  implementation(libs.kotlinx.coroutines.core)
  implementation(projects.packages.engine)
  implementation(libs.json)  // For JSON encode/decode support

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
