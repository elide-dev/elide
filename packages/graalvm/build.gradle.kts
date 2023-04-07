@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import kotlinx.benchmark.gradle.*

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("kapt")
  kotlin("plugin.allopen")
  id("dev.elide.build.native.lib")
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

group = "dev.elide"
version = rootProject.version as String


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

graalvmNative {
  agent {
    enabled.set(false)
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
      jmhVersion = "1.36"
    }
  }
}

micronaut {
  enableNativeImage(true)
  version.set(libs.versions.micronaut.lib.get())
  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "elide.runtime.*",
      "elide.runtime.gvm.*",
      "elide.runtime.gvm.internals.*",
      "elide.runtime.gvm.intrinsics.*",
    ))
  }
}

configurations["benchmarksImplementation"].extendsFrom(
  configurations.implementation.get(),
  configurations.testImplementation.get(),

)
configurations["benchmarksRuntimeOnly"].extendsFrom(
  configurations.runtimeOnly.get(),
  configurations.testRuntimeOnly.get()
)

dependencies {
  // API Deps
  api(libs.jakarta.inject)
  kapt(libs.micronaut.inject.java)

  // Modules
  api(project(":packages:base"))
  api(project(":packages:core"))
  implementation(project(":packages:ssr"))

  // Kotlin / KotlinX
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.reactor)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)

  // General
  implementation(libs.jimfs)
  implementation(libs.flatbuffers.java.core)
  implementation(libs.lmax.disruptor.core)
  implementation(libs.lmax.disruptor.proxy)

  // Compression
  implementation(libs.lz4)
  implementation(libs.brotli)
  implementation(libs.commons.compress)

  // Micronaut
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.test.junit5)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.cache.core)
  implementation(libs.micronaut.cache.caffeine)

  // SQLite
  implementation(libs.sqlite)

  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-protobuf"))
  implementation(project(":packages:proto:proto-kotlinx"))
  implementation(project(":packages:proto:proto-flatbuffers"))

  compileOnly(libs.graalvm.sdk)
  compileOnly(libs.graalvm.truffle.api)

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.micronaut.test.junit5)
  testCompileOnly(libs.graalvm.sdk)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
