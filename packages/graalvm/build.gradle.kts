@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")
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
}

dependencies {
  // Core platform versions.
  api(platform(project(":packages:bom")))

  // API Deps
  api(libs.jakarta.inject)
  api(libs.graalvm.sdk)

  // Modules
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))

  // KotlinX
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Google
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
  testImplementation(libs.graalvm.sdk)
}
