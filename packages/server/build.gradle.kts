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

import org.apache.tools.ant.taskdefs.condition.Os
import elide.internal.conventions.elide
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  
  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)

  id(libs.plugins.ksp.get().pluginId)
  id("elide.internal.conventions")
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
    ksp = true
  }

  java {
    configureModularity = false
  }

  native {
    target = NativeTarget.LIB
    useAgent = false
  }
}

group = "dev.elide"
version = rootProject.version as String

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

  // Modules
  api(projects.packages.base)
  api(projects.packages.core)
  api(projects.packages.model)
  api(projects.packages.ssr)
  api(projects.packages.graalvm)

  // KSP
  ksp(mn.micronaut.inject)
  ksp(mn.micronaut.inject.kotlin)

  // General
  implementation(libs.jackson.core)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.module.kotlin)
  implementation(mn.micronaut.jackson.databind)

  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  implementation(projects.packages.proto.protoKotlinx)

  // Crypto
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)

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
  implementation(mn.micronaut.http)
  implementation(mn.micronaut.http.server)
  implementation(mn.micronaut.http.server.netty)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.inject)
  implementation(mn.micronaut.inject.java)
  implementation(mn.micronaut.management)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactor)
  implementation(libs.kotlinx.coroutines.reactive)

  // General
  implementation(libs.reactivestreams)
  implementation(libs.google.common.html.types.types)

  // Runtime
  runtimeOnly(libs.jansi)
  runtimeOnly(mn.snakeyaml)

  // Netty: Native
  implementation(libs.netty.tcnative)

  val arch = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64", "aarch_64" -> "aarch_64"
    else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
  }
  when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> {
      implementation(libs.netty.tcnative.boringssl.static)
    }

    Os.isFamily(Os.FAMILY_UNIX) -> {
      when {
        Os.isFamily(Os.FAMILY_MAC) -> {
          implementation(libs.netty.transport.native.kqueue)
          implementation(libs.netty.transport.native.kqueue)
          implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
          implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
          implementation(libs.netty.resolver.dns.native.macos)
        }

        else -> {
          implementation(libs.netty.transport.native.epoll)
          implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-$arch") })
        }
      }
    }

    else -> {}
  }

  // Testing
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(mn.micronaut.test.junit5)
  testImplementation(projects.packages.test)
  testImplementation(kotlin("test-junit5"))
}

tasks {
  withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
      configureEach {
        includes.from("module.md")
      }
    }
  }

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
