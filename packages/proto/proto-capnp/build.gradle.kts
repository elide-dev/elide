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

plugins {
  kotlin("jvm")
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "proto-capnp"
    name = "Elide Protocol: Cap'n'Proto"
    description = "Elide protocol implementation for Cap'n'Proto."
    
    publish("maven") {
      from(components["kotlin"])
    }
  }
  
  kotlin { target = KotlinTarget.JVM }
  jvm { forceJvm17 = true }

  // disable module-info processing (not present)
  java {
    configureModularity = false
    includeJavadoc = false
  }
}

configurations {
  // `capnpInternal` uses the Cap'N'Proto implementation only, rather than the full cruft of Protocol Buffers non-lite.
  create("capnpInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
  }

  create("capnproto") {
    isCanBeResolved = true
    isCanBeConsumed = false
  }
}

val capnproto: Configuration by configurations.getting

artifacts {
  add("capnpInternal", tasks.jar)
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

  // Variant: Cap'n'Proto
  api(projects.packages.proto.protoCore)
  api(libs.capnproto.runtime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))

  capnproto(libs.capnproto.compiler)
}

afterEvaluate {
  tasks.named("runKtlintCheckOverMainSourceSet").configure {
    enabled = false
  }
}
