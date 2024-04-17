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

@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import io.gitlab.arturbosch.detekt.Detekt
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  alias(libs.plugins.protobuf)

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "proto-protobuf"
    name = "Elide Protocol: Protobuf"
    description = "Elide protocol implementation for Protocol Buffers."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
  }

  jvm {
    forceJvm17 = true
  }

  java {
    includeJavadoc = false
  }

  checks {
    disableAllChecks()
  }
}

tasks.withType(Detekt::class.java) {
  jvmTarget = "17"  // @TODO pull from property state
}

sourceSets {
  /**
   * Variant: Protocol Buffers
   */
  val main by getting {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
  val test by getting
}

configurations {
  // `modelInternal` is the dependency used internally by other Elide packages to access the protocol model. at present,
  // the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.builtins {
        id("kotlin")
      }
    }
  }
}

tasks {
  test {
    useJUnitPlatform()
    dependsOn(generateTestProto)
  }

  jar {
    dependsOn(generateProto)

    manifest {
      attributes["Automatic-Module-Name"] = "elide.protocol.protobuf"
    }
  }

  compileJava {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
      listOf(
        "--add-exports=elide.protocol.core/elide.proto=elide.protocol.protobuf",
        "--add-exports=elide.protocol.core/elide.proto.internal.annotations=elide.protocol.protobuf",
        "--add-reads=elide.protocol.protobuf=ALL-UNNAMED",
        "-nowarn",
        "-XDenableSunApiLintControl",
        "-Xlint:-deprecation",
      )
    })
  }

  compileKotlin {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs.plus(listOf(
        "-nowarn",
      ))
    }
  }
}

dependencies {
  // API
  api(libs.kotlinx.datetime)
  api(projects.packages.proto.protoCore)
  api(libs.protobuf.java)
  api(libs.protobuf.util)
  api(libs.protobuf.kotlin)

  // Implementation
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(projects.packages.core)
  implementation(libs.google.common.html.types.proto) {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "com.google.protobuf", module = "protobuf-util")
  }
  api(libs.google.common.html.types.types) {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "com.google.protobuf", module = "protobuf-util")
  }

  // Compile-only
  compileOnly(libs.google.cloud.nativeImageSupport)

  // Test
  testImplementation(projects.packages.test)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(projects.packages.proto.protoTest)
}
