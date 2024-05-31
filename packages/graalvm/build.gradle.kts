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
val oracleGvm = false
val enableTransportV2 = true
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
  }

  checks {
    spotless = true
    diktat = false
    ktlint = false
  }
}

kover {
  excludeInstrumentation {
    classes(
      "elide.runtime.gvm.js.JavaScript",
      "elide.runtime.gvm.internals.intrinsics.NativeRuntime",
    )
    packages(
      "elide.runtime.feature",
      "elide.runtime.feature.*"
    )
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

val jvmDefs = mapOf(
  "elide.nativeTransport.v2" to enableTransportV2.toString(),
)

val initializeAtRunTime = listOfNotNull(
  "com.sun.jna.platform.mac.CoreFoundation",
  "oshi.hardware.platform.linux",
  "oshi.hardware.platform.mac",
  "oshi.hardware.platform.mac.MacFirmware",
  "oshi.hardware.platform.unix",
  "oshi.hardware.platform.unix.aix",
  "oshi.hardware.platform.unix.freebsd",
  "oshi.hardware.platform.unix.openbsd",
  "oshi.hardware.platform.unix.solaris",
  "oshi.hardware.platform.windows",
  "oshi.jna.platform.mac.IOKit",
  "oshi.jna.platform.mac.SystemB",
  "oshi.jna.platform.mac.SystemConfiguration",
  "oshi.software.os",
  "oshi.software.os.linux",
  "oshi.software.os.linux.LinuxOperatingSystem",
  "oshi.software.os.mac",
  "oshi.software.os.mac.MacOperatingSystem",
  "oshi.software.os.unix.aix",
  "oshi.software.os.unix.aix.AixOperatingSystem",
  "oshi.software.os.unix.freebsd",
  "oshi.software.os.unix.freebsd.FreeBsdOperatingSystem",
  "oshi.software.os.unix.openbsd",
  "oshi.software.os.unix.openbsd.OpenBsdOperatingSystem",
  "oshi.software.os.unix.solaris",
  "oshi.software.os.unix.solaris.SolarisOperatingSystem",
  "oshi.software.os.windows",
  "oshi.software.os.windows.WindowsOperatingSystem",
  "oshi.util.platform.linux.DevPath",
  "oshi.util.platform.linux.ProcPath",
  "oshi.util.platform.linux.SysPath",
  "oshi.util.platform.mac.CFUtil",
  "oshi.util.platform.mac.SmcUtil",
  "oshi.util.platform.mac.SysctlUtil",
  "oshi.util.platform.unix.freebsd.BsdSysctlUtil",
  "oshi.util.platform.unix.freebsd.ProcstatUtil",
  "oshi.util.platform.unix.openbsd.FstatUtil",
  "oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil",
  "oshi.util.platform.unix.solaris.KstatUtil",
)

val sharedLibArgs = listOfNotNull(
  "--verbose",
  "--initialize-at-build-time=",
  "--initialize-at-run-time=${initializeAtRunTime.joinToString(",")}",
  "-H:+ReportExceptionStackTraces",
  if (oracleGvm) "-H:+AuxiliaryEngineCache" else null,
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
).plus(
  jvmDefs.flatMap { (k, v) ->
    listOf(
      "-D$k=$v",
      "-J-D$k=$v",
    )
  }
)

graalvmNative {
  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    trackReflectionMetadata = true
    enableExperimentalPredefinedClasses = true
    enableExperimentalUnsafeAllocationTracing = true
    enabled = System.getenv("GRAALVM_AGENT") == "true"

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.addAll(listOf("run", "optimizedRun", "test"))
      if (gradle.startParameter.taskNames.contains("test") || properties.containsKey("agentTest")) {
        outputDirectories.add("src/test/resources/META-INF/native-image")
      } else {
        outputDirectories.add("src/main/resources/META-INF/native-image")
      }
      mergeWithExisting = true
    }
  }

  binaries {
    create("shared") {
      sharedLibrary = true
      buildArgs(sharedLibArgs)
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      buildArgs(sharedLibArgs)
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
  api(projects.packages.engine)

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
  api(libs.graalvm.js.language)
  compileOnly(libs.graalvm.svm)

  if (oracleGvm) {
    api(libs.graalvm.polyglot.js)
  } else {
    api(libs.graalvm.polyglot.js.community)
  }

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(mn.micronaut.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(projects.packages.graalvmPy)

  if (enableTransportV2) {
    // Testing: Native Transports
    testImplementation(projects.packages.transport.transportEpoll)
    testImplementation(projects.packages.transport.transportKqueue)
    testImplementation(libs.netty.transport.native.classes.kqueue)
    testImplementation(libs.netty.transport.native.classes.epoll)
  } else {
    testImplementation(libs.netty.transport.native.kqueue)
    testImplementation(libs.netty.transport.native.epoll)
  }
}

// Configurations: Testing
val testBase: Configuration by configurations.creating

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
    systemProperty("elide.js.vm.enableStreams", "true")
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
