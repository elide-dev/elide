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

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  
  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "server"
    name = "Elide for Servers"
    description = "Server-side tools, framework, and runtime, based on GraalVM and Micronaut."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
    kapt = true
  }

  java {
    configureModularity = false
  }

  native {
    target = NativeTarget.LIB
    useAgent = false
  }

  checks {
    spotless = false
  }
}

group = "dev.elide"

micronaut {
  version = libs.versions.micronaut.lib.get()

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.server",
      "elide.server.*",
      "elide.server.annotations",
      "elide.server.annotations.*",
    ))
  }
}

dependencies {
  // BOMs
  api(platform(libs.netty.bom))

  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)
  api(libs.guava)
  api(mn.micronaut.core)
  api(mn.micronaut.context)
  api(mn.micronaut.http)
  api(mn.micronaut.inject)
  api(mn.reactive.streams)
  api(libs.kotlinx.coroutines.core.jvm)
  api(libs.kotlinx.html.jvm)
  api(libs.kotlinx.serialization.core.jvm)
  api(libs.protobuf.java)
  api(projects.packages.proto.protoProtobuf)

  // Modules
  api(projects.packages.base)
  api(projects.packages.ssr)
  api(projects.packages.graalvm)

  // KSP (test-only)
  testAnnotationProcessor(mn.micronaut.inject)
  testAnnotationProcessor(mn.micronaut.inject.java)

  // General
  implementation(libs.graalvm.polyglot)
  implementation(mn.reactor)

  // Crypto
  implementation(libs.bouncycastle)
  implementation(libs.conscrypt)

  // KotlinX
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactor)

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.css)

  // Protocol Buffers
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // Brotli (not in use yet)
  //  implementation(libs.brotli)
  //  implementation(libs.brotli.native.osx)
  //  implementation(libs.brotli.native.osx.amd64)
  //  implementation(libs.brotli.native.osx.arm64)
  //  implementation(libs.brotli.native.linux)
  //  implementation(libs.brotli.native.linux.amd64)
  //  implementation(libs.brotli.native.linux.arm64)
  //  implementation(libs.brotli.native.windows)
  //  implementation(libs.brotli.native.windows.amd64)

  // Micronaut
  implementation(mn.micronaut.aop)
  implementation(mn.micronaut.http.netty)
  implementation(mn.micronaut.jackson.databind)

  // General
  implementation(libs.reactivestreams)

  // Runtime
  runtimeOnly(libs.jansi)
  runtimeOnly(mn.snakeyaml)
  runtimeOnly(libs.jackson.core)
  runtimeOnly(libs.jackson.databind)
  runtimeOnly(libs.jackson.module.kotlin)
  implementation(mn.micronaut.http.server)
  implementation(mn.micronaut.http.server.netty)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(mn.micronaut.test.junit5)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(mn.micronaut.test.core)
  testApi(project(":packages:engine", configuration = "testInternals"))
}

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
  }
}

val commonNativeArgs = listOf(
  "--gc=serial",
  "-H:+AuxiliaryEngineCache",
)

val initializeAtBuildTime = listOf(
  "kotlin.DeprecationLevel",
  "kotlin.coroutines.intrinsics.CoroutineSingletons",
  "kotlin.annotation.AnnotationRetention",
  "kotlin.annotation.AnnotationTarget",
  "ch.qos.logback",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",
  "org.codehaus.stax2.typed.Base64Variants",
  "org.bouncycastle.util.Properties",
  "org.bouncycastle.util.Strings",
  "org.bouncycastle.crypto.macs.HMac",
  "org.bouncycastle.crypto.prng.drbg.Utils",
  "org.bouncycastle.jcajce.provider.drbg.DRBG",
  "org.bouncycastle.jcajce.provider.drbg.DRBG$${'$'}Default",
  "org.bouncycastle.jcajce.provider.drbg.DRBG${'$'}NonceAndIV",
).map {
  "--initialize-at-build-time=$it"
}

val initializeAtRuntime = listOf(
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
  "com.sun.tools.javac.file.Locations",
).map {
  "--initialize-at-run-time=$it"
}

val initializeAtBuildTimeTest = initializeAtBuildTime.plus(listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
).map {
  "--initialize-at-build-time=$it"
})

val initializeAtRuntimeTest = emptyList<String>().map {
  "--initialize-at-run-time=$it"
}

graalvmNative {
  binaries {
    named("main") {
      sharedLibrary = true
      buildArgs(commonNativeArgs.plus(initializeAtBuildTime).plus(initializeAtRuntime))
    }

    named("test") {
      quickBuild = true
      buildArgs(commonNativeArgs.plus(initializeAtBuildTimeTest).plus(initializeAtRuntimeTest))
    }
  }
}
