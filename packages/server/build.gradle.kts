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

micronaut {
  version.set(libs.versions.micronaut.lib.get())

  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "elide.server",
      "elide.server.*",
      "elide.server.annotations",
      "elide.server.annotations.*",
    ))
  }
}

dependencies {
  // API Deps
  api(libs.jakarta.inject)
  api(libs.graalvm.sdk)

  // Modules
  kapt(libs.micronaut.inject)
  kapt(libs.micronaut.inject.java)
  implementation(libs.slf4j)
  implementation(project(":packages:base"))
  implementation(project(":packages:proto"))

  // Crypto
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)

  // Reactive Java
  implementation(libs.reactor.core)
  implementation(libs.reactor.netty.core)
  implementation(libs.reactor.netty.http)

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.css)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // Brotli
  implementation(libs.brotli)
  implementation(libs.brotli.native.osx)
  implementation(libs.brotli.native.linux)
  implementation(libs.brotli.native.windows)

  // Micronaut
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.http.server)
  implementation(libs.micronaut.http.server.netty)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.protobuf)
  implementation(libs.micronaut.management)
  implementation(libs.micronaut.views.core)

  // Netty: Native
  implementation(libs.netty.tcnative)
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(libs.netty.transport.native.unixCommon)
  implementation(libs.netty.transport.native.epoll)
  implementation(libs.netty.transport.native.kqueue)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactive)

  // General
  implementation(libs.reactivestreams)
  implementation(libs.google.common.html.types.types)

  // Testing
  kaptTest(libs.micronaut.inject)
  kaptTest(libs.micronaut.inject.java)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(libs.micronaut.test.junit5)
  testImplementation(kotlin("test"))
  testImplementation(project(":packages:test"))
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
  dokkaSourceSets {
    configureEach {
      includes.from("module.md")
//      samples.from("samples/basic.kt")
    }
  }
}
