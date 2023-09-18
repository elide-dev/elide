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

import elide.internal.conventions.elide
import elide.internal.conventions.publishing.publish
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.kotlin.*

plugins {
  kotlin("multiplatform")
  id("elide.internal.conventions")
}

val buildWasm = project.properties["buildWasm"] == "true"

elide {
  publishing {
    id = "proto-core"
    name = "Elide Protocol: API"
    description = "API headers and services for the Elide Protocol."
  }

  kotlin {
    target = (KotlinTarget.JVM + KotlinTarget.JsNode).let {
      if(buildWasm) it + KotlinTarget.WASM else it
    }
  }

  jvm {
    forceJvm17 = true
  }

  // disable module-info processing (not present)
  java {
    configureModularity = false
  }
}

dependencies {
  common {
    api(libs.kotlinx.datetime)
    implementation(projects.packages.core)
    implementation(projects.packages.base)
  }

  commonTest {
    implementation(projects.packages.test)
  }

  jvm {
    implementation(kotlin("stdlib-jdk8"))
  }

  jvmTest {
    implementation(libs.truth)
    implementation(libs.truth.java8)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.jupiter.engine)
  }
}

// Configurations: Testing
val testBase: Configuration by configurations.creating {}

tasks {
  val testJar by registering(Jar::class) {
    description = "Base (abstract) test classes for all implementations"
    archiveClassifier = "tests"
    from(sourceSets.named("test").get().output)
  }

  artifacts {
    add("testBase", testJar)
  }
}
