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
import io.netifi.flatbuffers.plugin.tasks.FlatBuffers

plugins {
  kotlin("jvm")
  alias(libs.plugins.flatbuffers)

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "proto-flatbuffers"
    name = "Elide Protocol: Flatbuffers"
    description = "Elide protocol implementation for Flatbuffers"

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
  }

  java {
    configureModularity = false
  }
}

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

configurations {
  // `flatInternal` uses the flatbuffers implementation only, rather than the full cruft of Protocol Buffers non-lite.
  create("flatInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
  }
}

flatbuffers {
  language = "kotlin"
}

val compileFlatbuffers by tasks.creating(FlatBuffers::class) {
  description = "Generate Flatbuffers code for Kotlin/JVM"
  inputDir = file("${rootProject.projectDir}/proto")
  outputDir = file("$projectDir/src/main/flat")
}

artifacts {
  add("flatInternal", tasks.jar)
}

dependencies {
  // Common
  api(libs.kotlinx.datetime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(projects.packages.core)
  implementation(projects.packages.base)
  testImplementation(projects.packages.test)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)

  // Variant: Flatbuffers
  api(projects.packages.proto.protoCore)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.flatbuffers.java.core)
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))
}

afterEvaluate {
  tasks.named("runKtlintCheckOverMainSourceSet").configure {
    enabled = false
  }
}
