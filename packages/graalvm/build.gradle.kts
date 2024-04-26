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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
  "COMPATIBILITY_WARNING",
)

import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import kotlinx.benchmark.gradle.benchmark
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)
  id("org.graalvm.buildtools.native")

  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)

  id("elide.internal.conventions")
}


group = "dev.elide"
version = rootProject.version as String

val enableJpms = false
val ktCompilerArgs = emptyList<String>()
val javacArgs = listOf(
  "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
)

elide {
  publishing {
    id = "graalvm"
    name = "Elide for GraalVM"
    description = "Integration package with GraalVM and GraalJS."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  java {
    configureModularity = enableJpms
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
    kapt = true
  }

  native {
    target = NativeTarget.LIB
    useAgent = false
  }

  checks {
    spotless = true
    diktat = false
    ktlint = false
  }
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
  if (enableJpms) modularity.inferModulePath = true
}

sourceSets {
  val main by getting {
    java.srcDirs(
      layout.projectDirectory.dir("src/main/java9")
    )
  }
  val benchmarks by creating {
    kotlin.srcDirs(
      layout.projectDirectory.dir("src/benchmarks/kotlin"),
      layout.projectDirectory.dir("src/main/kotlin"),
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

val benchmarksCompileClasspath: Configuration by configurations.getting {
  extendsFrom(
    configurations.compileClasspath.get(),
  )
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
  // KSP
  kapt(mn.micronaut.inject.java)

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

  // General
  implementation(libs.jimfs)
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

  // OSHI (System Information)
  implementation(libs.oshi.core)

  // Netty
  implementation(libs.netty.codec.http)
  implementation(libs.netty.codec.http2)

  // Micrometer
  implementation(mn.micrometer.core)
  implementation(mn.micrometer.observation)

  val arch = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64", "aarch_64" -> "aarch_64"
    else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
  }
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(libs.netty.transport.native.kqueue)
  implementation(libs.netty.transport.native.kqueue)
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
  implementation(libs.netty.resolver.dns.native.macos)

  implementation(libs.netty.transport.native.epoll)
  implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-$arch") })
  implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-$arch") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-$arch") })

  // SQLite
  implementation(libs.sqlite)

  implementation(libs.protobuf.java)
  implementation(libs.protobuf.kotlin)
  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  implementation(projects.packages.proto.protoKotlinx)
  implementation(libs.capnproto.runtime)
  implementation(libs.capnproto.runtime.rpc)

  api(libs.graalvm.polyglot)
  api(libs.graalvm.polyglot.js.community)
  compileOnly(libs.graalvm.svm)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(mn.micronaut.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)

  testImplementation(projects.packages.graalvmPy)
}

// Configurations: Testing
val testBase: Configuration by configurations.creating

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
  }

  javadoc {
    enabled = false
  }

  jar {
    manifest {
      attributes(
        "Elide-Engine-Version" to "v3",
        "Elide-Release-Track" to "ALPHA",
        "Elide-Release-Version" to version,
        "Specification-Title" to "Elide VM Specification",
        "Specification-Version" to "0.1",
        "Implementation-Title" to "Elide VM Specification",
        "Implementation-Version" to "0.1",
        "Implementation-Vendor" to "Elide Technologies, Inc",
      )
    }
  }

  compileJava {
    options.javaModuleVersion = version as String
    if (enableJpms) modularity.inferModulePath = true

    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
      javacArgs
    })
  }

  /**
   * Variant: Testsuite
   */
  val testJar by registering(Jar::class) {
    description = "Base (abstract) test classes for all GraalVM modules"
    archiveClassifier = "tests"
    from(sourceSets.named("test").get().output)
  }

  artifacts {
    add("testBase", testJar)
  }
}
