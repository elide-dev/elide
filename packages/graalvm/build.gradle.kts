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
  "COMPATIBILITY_WARNING",
)

import ElidePackages.elidePackage
import kotlinx.benchmark.gradle.*

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("plugin.allopen")

  id("dev.elide.build.native.lib")
  id("dev.elide.build.publishable")

  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

group = "dev.elide"
version = rootProject.version as String

val encloseSdk = false

kotlin {
  explicitApi()
}

sourceSets {
  val benchmarks by creating {
    kotlin.srcDirs(
      "$projectDir/src/benchmarks/kotlin",
      "$projectDir/src/main/kotlin",
    )
  }
}

val initializeAtBuildTime = listOf(
  "kotlin.DeprecationLevel",
  "kotlin.annotation.AnnotationRetention",
  "kotlin.annotation.AnnotationTarget",
  "kotlin.coroutines.intrinsics.CoroutineSingletons",
)

val initializeAtBuildTimeTest = listOf(
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
  "org.junit.platform.launcher.core.LauncherConfig",
)

val sharedLibArgs = listOf(
  "-H:+AuxiliaryEngineCache",
)

graalvmNative {
  testSupport = true

  agent {
    enabled = false
  }

  binaries {
    create("shared") {
      sharedLibrary = true
      buildArgs(initializeAtBuildTime.map {
        "--initialize-at-build-time=$it"
      }.plus(sharedLibArgs))
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      buildArgs(initializeAtBuildTime.plus(initializeAtBuildTimeTest).map {
        "--initialize-at-build-time=$it"
      }.plus(sharedLibArgs))
    }
  }
}

benchmark {
  configurations {
    named("main") {
      warmups = 10
      iterations = 5
    }
  }
  targets {
    register("benchmarks") {
      this as JvmBenchmarkTarget
      jmhVersion = libs.versions.jmh.lib.get()
    }
  }
}

micronaut {
  enableNativeImage(true)
  version = libs.versions.micronaut.lib.get()
  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.runtime.*",
      "elide.runtime.gvm.*",
      "elide.runtime.gvm.internals.*",
      "elide.runtime.gvm.intrinsics.*",
    ))
  }
}

val benchmarksImplementation: Configuration by configurations.getting {
  extendsFrom(
    configurations.implementation.get(),
    configurations.testImplementation.get(),
  )
}
val benchmarksRuntimeOnly: Configuration by configurations.getting {
  extendsFrom(
    configurations.runtimeOnly.get(),
    configurations.testRuntimeOnly.get()
  )
}

dependencies {
  // API Deps
  api(libs.jakarta.inject)

  // Modules
  api(projects.packages.base)
  api(projects.packages.core)
  api(projects.packages.ssr)

  // Kotlin / KotlinX
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.collections.immutable.jvm)
  runtimeOnly(libs.kotlinx.coroutines.reactor)

  // General
  implementation(libs.jimfs)
  implementation(libs.lmax.disruptor.core)
  implementation(libs.jackson.core)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.module.kotlin)
  implementation(mn.micronaut.jackson.databind)

  // Compression
  implementation(libs.commons.compress)
  implementation(libs.xz)
  implementation(libs.zstd)

  // Micronaut
  runtimeOnly(mn.micronaut.graal)
  implementation(mn.micronaut.http)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.inject)

  // Netty
  implementation(libs.reactor.netty)
  implementation(libs.netty.codec.http)
  implementation(libs.netty.codec.http2)

  // SQLite
  implementation(libs.sqlite)

  implementation(libs.protobuf.java)
  implementation(libs.protobuf.kotlin)
  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  implementation(projects.packages.proto.protoKotlinx)
  implementation(projects.packages.proto.protoFlatbuffers)

  if (encloseSdk) {
    compileOnly(libs.graalvm.sdk)
    compileOnly(libs.graalvm.truffle.api)
  }

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(mn.micronaut.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)

  if (encloseSdk) {
    testCompileOnly(libs.graalvm.sdk)
  }
}

elidePackage(
  id = "graalvm",
  name = "Elide for GraalVM",
  description = "Integration package with GraalVM and GraalJS.",
)

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
  }
}
