@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.jvm")
}

group = "dev.elide.tools"
version = rootProject.version as String


kotlin {
  explicitApi()

  publishing {
    publications {
      create<MavenPublication>("maven") {
        artifactId = "processor"
        groupId = "dev.elide.tools"
        version = rootProject.version as String
      }
    }
  }
}

dependencies {
  // Core platform versions.
  api(platform(project(":packages:platform")))
  api(project(":packages:proto"))

  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)
  api(libs.graalvm.sdk)

  // Modules
  implementation(project(":packages:base"))

  // KSP
  implementation(libs.ksp)
  implementation(libs.kotlinx.atomicfu)

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactive)

  // Testing
  testImplementation(kotlin("test"))
  testImplementation(project(":packages:test"))
}
