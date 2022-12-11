@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("kapt")
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String


kotlin {
  explicitApi()
}

graalvmNative {
  agent {
    enabled.set(false)
  }
}

micronaut {
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

dependencies {
  // Core platform versions.
  implementation(platform(project(":packages:platform")))

  // API Deps
  api(libs.jakarta.inject)
  api(libs.graalvm.sdk)
  kapt(libs.micronaut.inject.java)

  // Modules
  api(project(":packages:ssr"))
  api(project(":packages:base"))
  api(project(":packages:core"))
  api(project(":packages:proto"))
  api(project(":packages:model"))
  api(project(":packages:server"))

  // KotlinX
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // General
  implementation(libs.guava)

  // Micronaut
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.cache.core)
  implementation(libs.micronaut.cache.caffeine)

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.micronaut.test.junit5)
  testImplementation(libs.graalvm.sdk)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
