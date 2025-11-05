/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
  "MagicNumber"
)

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish
import elide.toolchain.host.Criteria
import elide.toolchain.host.TargetCriteria
import elide.toolchain.host.TargetInfo
import elide.toolchain.host.TargetPredicate

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")

  alias(libs.plugins.micronaut.minimal.library)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.graalvm)
  alias(libs.plugins.jmh)
  alias(libs.plugins.ksp)
  alias(libs.plugins.elide.conventions)
}

group = "dev.elide"
version = rootProject.version as String

// Flags affecting this build script:
//
// - `elide.release`: true/false
// - `elide.buildMode`: `dev`, `release`, `debug`
// - `elide.target`: Known target, like `linux-amd64`, `linux-amd64-musl`, `darwin-amd64`, or `windows-amd64`
// - `elide.targetOs`: `darwin`, `linux`, `windows`
// - `elide.targetArch`: `amd64`, `arm64`
// - `elide.targetLibc`: `glibc` or `musl`
// - `elide.march`: `native`, `compatibility`
// - `elide.compiler`: Custom compiler name or path
// - `elide.linker`: Custom linker name or path
//
// Environment respected by this script:
//
// - `CC`: C compiler
// - `LD`: Linker
// - `CFLAGS`: C compiler flags

val oracleGvm = true
val oracleGvmLibs = oracleGvm
val enableJpms = false
val enableEdge = false
val enableSqlite = true
val enableStaticJni = true
val enableToolchains = true
val enableTransportV2 = false
val ktCompilerArgs = emptyList<String>()
val javacArgs = listOf(
  "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
)

// Java Launcher (GraalVM at either EA or LTS)
val edgeJvmTarget = 25
val stableJvmTarget = 25
val edgeJvm = JavaVersion.toVersion(edgeJvmTarget)
val stableJvm = JavaVersion.toVersion(stableJvmTarget)
val selectedJvmTarget = if (enableEdge) edgeJvmTarget else stableJvmTarget
val selectedJvm = if (enableEdge) edgeJvm else stableJvm
val elideTarget = TargetInfo.current(project)

val jvmType: JvmVendorSpec =
  if (oracleGvm) JvmVendorSpec.ORACLE else JvmVendorSpec.GRAAL_VM

val gvmLauncher = javaToolchains.launcherFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

val gvmCompiler = javaToolchains.compilerFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

val isRelease = !quickbuild && (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)

val nativesType = if (isRelease) "release" else "debug"

elide {
  publishing {
    id = "graalvm"
    name = "Elide for GraalVM"
    description = "Integration package with GraalVM and GraalJS."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  jvm {
    alignVersions = true
  }

  java {
    configureModularity = enableJpms
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
    ksp = true
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
  currentProject {
    instrumentation {
      excludedClasses.addAll(listOf(
        "elide.runtime.gvm.js.JavaScript",
        "elide.runtime.gvm.internals.intrinsics.NativeRuntime",
        "elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature",
        "elide.runtime.feature.engine.NativeConsoleFeature",
        "elide.runtime.feature.engine.NativeCryptoFeature",
        "elide.runtime.feature.engine.NativeSQLiteFeature",
        "elide.runtime.feature.engine.NativeTransportFeature",
      ))
    }
  }
}

java {
  if (enableJpms) modularity.inferModulePath = true
}

sourceSets {
  val main by getting {
    java.srcDirs(
      layout.projectDirectory.dir("src/main/java9"),
    )
    kotlin.srcDirs(
      layout.projectDirectory.dir("src/main/kotlin"),
    )
  }
}

val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false
val pkgVersion = if (stamp) {
  libs.versions.elide.asProvider().get()
} else {
  "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
}

val umbrellaNativesPath: String =
  rootProject.layout.projectDirectory.dir("target/${elideTarget.triple}/$nativesType").asFile.path
val nativesPath = umbrellaNativesPath
val targetSqliteDir = rootProject.layout.projectDirectory.dir("third_party/sqlite/install")
val targetSqliteLibDir = targetSqliteDir.dir("lib")

val javaLibPath = provider {
  StringBuilder().apply {
    append(nativesPath)
    append(File.pathSeparator)
    append(targetSqliteLibDir)
    System.getProperty("java.library.path", "").let {
      if (it.isNotEmpty()) {
        append(File.pathSeparator)
        append(it)
      }
    }
  }
}

val jvmDefs = mapOf(
  "elide.natives" to nativesPath,
  "elide.target" to elideTarget.triple,
  "org.sqlite.lib.path" to nativesPath,
  "org.sqlite.lib.exportPath" to nativesPath,
  "elide.nativeTransport.v2" to enableTransportV2.toString(),
  "java.library.path" to javaLibPath.get(),
)

val testJvmDefs = jvmDefs.plus(listOf(
  "elide.js.preloadModules" to "false",
  "elide.js.vm.enableStreams" to "true",
  "java.util.concurrent.ForkJoinPool.common.parallelism" to "4",
))

val initializeAtRunTime = listOfNotNull(
  "elide.runtime.intrinsics.server.http.netty.NettyRequestHandler",
  "elide.runtime.intrinsics.server.http.netty.NettyHttpResponse",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport",
  "elide.runtime.intrinsics.server.http.netty.KQueueTransport",
  "elide.runtime.intrinsics.server.http.netty.EpollTransport",
  "elide.runtime.intrinsics.server.http.netty.IOUringTransport",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport",
  "elide.runtime.intrinsics.server.http.netty.NettyTransport${'$'}Companion",
  "elide.runtime.gvm.internals.node.process.NodeProcess${'$'}NodeProcessModuleImpl",
  "io.netty.buffer.AbstractReferenceCountedByteBuf",
  "io.netty.buffer.PooledByteBufAllocator",
  "io.netty.buffer.ByteBufAllocator",
  "io.netty.buffer.ByteBufUtil",
  "io.netty.buffer.AbstractReferenceCountedByteBuf",
  "io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider",
  "io.netty.resolver.dns.DnsServerAddressStreamProviders${'$'}DefaultProviderHolder",
  "io.netty.resolver.dns.DnsNameResolver",
  "io.netty.resolver.HostsFileEntriesResolver",
  "io.netty.resolver.dns.ResolvConf${'$'}ResolvConfLazy",
  "io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider",
  "io.netty.handler.codec.http2.Http2CodecUtil",
  "io.netty.handler.codec.http2.Http2ClientUpgradeCodec",
  "io.netty.handler.codec.http2.Http2ConnectionHandler",
  "io.netty.handler.codec.http2.DefaultHttp2FrameWriter",
  "io.netty.incubator.channel.uring",
  "io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  "io.netty.incubator.channel.uring.Native",
  "io.netty.handler.codec.http.HttpObjectEncoder",
  "io.netty.internal.tcnative.SSL",
  "io.micronaut.core.util.KotlinUtils",
  "io.micronaut.core.io.socket.SocketUtils",
  "io.micronaut.core.type.RuntimeTypeInformation${'$'}LazyTypeInfo",
  "io.micronaut.context.env.CachedEnvironment",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver${'$'}LazyInit",
  "com.sun.jna.platform.mac.CoreFoundation",
  "com.sun.jna.Structure${'$'}FFIType",
  "com.sun.jna.platform.mac.IOKit",
  "com.sun.jna.platform.mac.IOKitUtil",
  "com.sun.jna.platform.mac.SystemB",
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
  "oshi.util.platform.mac.CFUtil",
  "oshi.util.platform.mac.SmcUtil",
  "oshi.util.platform.mac.SysctlUtil",
  "oshi.util.platform.unix.freebsd.BsdSysctlUtil",
  "oshi.util.platform.unix.freebsd.ProcstatUtil",
  "oshi.util.platform.unix.openbsd.FstatUtil",
  "oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil",
  "oshi.util.platform.unix.solaris.KstatUtil",
  "com.sun.jna.NativeLibrary",
  "com.sun.jna.NativeLibrary\$1",
  "com.sun.jna.internal.Cleaner\$CleanerRef",
)

val initializeAtRunTimeTest = initializeAtRunTime.plus(listOfNotNull<String>())

val initializeAtBuildTime = listOf(
  "kotlinx.atomicfu",
  "io.micronaut.context.DefaultApplicationContextBuilder",
  "elide.runtime.core.internals.graalvm.GraalVMEngine\$Companion",
  "org.fusesource.jansi.io.AnsiOutputStream",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap\$1",
  "com.google.common.base.Equivalence\$Equals",
  "kotlin",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueEntry\$Helper",
  "ch.qos.logback",
  "elide.runtime.gvm.internals.sqlite",
  "elide.runtime.gvm.internals.sqlite.SqliteModule",
  "elide.runtime.lang.javascript.JavaScriptPrecompiler",
  "elide.runtime.gvm.intrinsics.BuildTimeIntrinsicsResolver",
  "java.sql",
  "org.slf4j",
  "org.sqlite",
  "org.pkl.core.runtime.VmLanguageProvider",
  "elide.runtime.lang.typescript",
  "elide.runtime.typescript",
  "org.xml.sax.helpers.LocatorImpl",
  "org.xml.sax.helpers.AttributesImpl",
  "oshi.software.os.linux.LinuxOperatingSystem",
)

val nativeEnabledModules = listOf(
  "org.graalvm.truffle",
  "ALL-UNNAMED",
)

val pluginApiHeader =
  rootProject.layout.projectDirectory.file("crates/substrate/headers/elide-plugin.h").asFile.path

val sharedLibArgs = listOfNotNull(
  // "--verbose",
  // "-J-Xlog:library=info",
  "-H:+UnlockExperimentalVMOptions",
  "-Delide.staticJni=$enableStaticJni",
  "-Delide.natives.pluginApiHeader=$pluginApiHeader",
  "-J-Delide.staticJni=$enableStaticJni",
  "-Delide.staticUmbrella=$enableStaticJni",
  "-J-Delide.staticUmbrella=$enableStaticJni",
  "--enable-native-access=${nativeEnabledModules.joinToString(",")}",
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
).plus(
  javaLibPath.get().toString().let {
    listOf(
      "-Djava.library.path=$it",
      "-J-Djava.library.path=$it",
    )
  },
).plus(
  "--initialize-at-run-time=${initializeAtRunTimeTest.joinToString(",")}",
).plus(
  "--initialize-at-build-time=${initializeAtBuildTime.joinToString(",")}"
)

val testLibArgs = sharedLibArgs.plus(listOf(
  "--gc=G1",
  "-R:MaxHeapSize=4g",
  "-H:+AddAllCharsets",
  "-H:NativeLinkerOption=-lm",
  "-H:NativeLinkerOption=$nativesPath/libdiag.so",
  "-H:NativeLinkerOption=$nativesPath/libsqlitejdbc.so",
  "-H:NativeLinkerOption=$nativesPath/libumbrella.so",
  "-H:NativeLinkerOption=$nativesPath/libjs.so",
  "-H:NativeLinkerOption=$nativesPath/libposix.so",
  "-H:NativeLinkerOption=$nativesPath/libterminal.so",
))

val layerOut = layout.buildDirectory.file("native/nativeLayerCompile/elide-graalvm.nil")
val baseLayer = project(":packages:engine").layout.buildDirectory.file("native/nativeLayerCompile/elide-base.nil")

graalvmNative {
  testSupport = true
  useArgFile = true

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
      inputTaskNames = listOf("run", "optimizedRun")
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
      buildArgs(sharedLibArgs.plus("--shared"))
    }

    create("layer") {
      imageName = "libelidegraalvm"
      classpath(tasks.compileJava, tasks.compileKotlin, configurations.nativeImageClasspath)

      buildArgs(sharedLibArgs.plus(listOf(
        "-H:LayerCreate=${layerOut.get().asFile.name}"
      )))
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      jvmArgs("-Dpolyglot.engine.WarnInterpreterOnly=false")
      buildArgs(sharedLibArgs.plus(testLibArgs).plus(listOf(
        "--features=org.graalvm.junit.platform.JUnitPlatformFeature",
        "--verbose",
        "org.graalvm.junit.platform.NativeImageJUnitLauncher",
      )))
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

dependencies {
  // KSP
  ksp(mn.micronaut.inject.kotlin)
  ksp(projects.packages.engine)
  kspTest(mn.micronaut.inject.kotlin)
  annotationProcessor(libs.graalvm.truffle.processor)

  // API Deps
  api(libs.jakarta.inject)
  api(libs.guava)
  api(libs.kotlinx.io)
  api(mn.micronaut.core)

  // Modules
  api(projects.packages.base)
  api(projects.packages.core)
  api(projects.packages.ssr)
  api(projects.packages.localAi)
  api(projects.packages.engine)
  api(projects.packages.graalvmTs)
  api(projects.packages.graalvmJs)
  api(projects.packages.graalvmWasm)
  api(projects.packages.graalvmPy)
  api(projects.packages.secrets)

  // GraalVM / Truffle
  api(libs.graalvm.truffle.api)
  api(libs.graalvm.truffle.runtime)

  // Kotlin / KotlinX
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.collections.immutable.jvm)

  // Brotli
  api(libs.brotli)
  implementation(libs.brotli.native.osx)
  implementation(libs.brotli.native.osx.amd64)
  implementation(libs.brotli.native.osx.arm64)
  implementation(libs.brotli.native.linux)
  implementation(libs.brotli.native.linux.amd64)
  implementation(libs.brotli.native.linux.arm64)
  implementation(libs.brotli.native.windows)
  implementation(libs.brotli.native.windows.amd64)

  // General
  implementation(libs.jimfs)
  implementation(libs.mordant)

  // Compression
  api(libs.commons.compress)

  // Micronaut
  runtimeOnly(mn.micronaut.graal)
  implementation(mn.micronaut.http)
  implementation(mn.micronaut.context)

  // OSHI (System Information)
  implementation(libs.oshi.core)

  // Netty
  implementation(libs.netty.codec.http)
  implementation(libs.netty.codec.http2)

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
  if (enableSqlite) implementation(projects.packages.sqlite)
  else compileOnly(projects.packages.sqlite)

  api(libs.graalvm.polyglot)
  api(libs.graalvm.js.language)
  compileOnly(libs.graalvm.svm)

  if (oracleGvmLibs) {
    api(libs.graalvm.polyglot.js)
  } else {
    api(libs.graalvm.polyglot.js.community)
  }

  implementation(platform(libs.aws.sdk.bom))
  implementation(libs.aws.sdk.s3)

  // Testing
  testApi(project(":packages:engine", configuration = "testInternals"))
  testApi(libs.graalvm.truffle.api)
  testApi(libs.graalvm.truffle.runtime)
  testImplementation(libs.locals3)
  testImplementation(libs.jackson.core)
  testImplementation(libs.jackson.databind)
  testImplementation(libs.jackson.module.kotlin)
  testImplementation(mn.micronaut.jackson.databind)
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(mn.micronaut.test.junit5)
  testImplementation(projects.packages.graalvmJs)
  testImplementation(projects.packages.graalvmTs)
  testImplementation(projects.packages.graalvmWasm)
  testImplementation(projects.packages.graalvmPy)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.jna.jpms)

  testImplementation(libs.netty.tcnative.boringssl.static)
  testImplementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-x86_64") })
  testImplementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-aarch_64") })
  testImplementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-x86_64") })
  testImplementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-aarch_64") })
  testImplementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
  testImplementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })

  if (enableTransportV2) {
    // Testing: Native Transports
    implementation(project(":packages:transport:transport-epoll"))
    implementation(project(":packages:transport:transport-kqueue"))
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
    // must be root project so that test scripts can be resolved during smoke tests
    workingDir = rootProject.layout.projectDirectory.asFile
    maxHeapSize = "8G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.natives", nativesPath)
    systemProperty("java.library.path", javaLibPath.get().toString())
    testJvmDefs.forEach {
      systemProperty(it.key, it.value)
    }
    jvmArgs.add("--enable-native-access=ALL-UNNAMED")
    if (enableToolchains) javaLauncher = gvmLauncher
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

    options.compilerArgs = (options.compilerArgs ?: emptyList())
    options.compilerArgs.addAll(javacArgs)

    if (enableToolchains) javaCompiler = gvmCompiler
  }

  compileTestJava {
    options.javaModuleVersion = version as String
    if (enableJpms) modularity.inferModulePath = true

    options.compilerArgs = (options.compilerArgs ?: emptyList())
    options.compilerArgs.addAll(javacArgs)

    if (enableToolchains) javaCompiler = gvmCompiler
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

  named("nativeLayerCompile").configure {
    dependsOn(":packages:engine:nativeLayerCompile")
  }
}

val (jsGroup, jsName) = libs.graalvm.js.language.get().let {
  it.group to it.name
}
configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("${jsGroup}:${jsName}")).apply {
      using(project(":packages:graalvm-js"))
      because("Uses Elide's patched version of GraalJs")
    }
  }
}

val jobs = Runtime.getRuntime().availableProcessors() / 2

val buildForNative = when {
  (findProperty("elide.native") as? String) != null -> "yes"
  (findProperty("elide.arch") as? String) != null -> if (project.properties["elide.arch"] as String == "native") {
    "yes"
  } else "no"
  else -> "no"
}

val thirdPartyDir: String =
  rootProject.layout.projectDirectory.dir("third_party").asFile.absolutePath

val buildThirdPartyNatives by tasks.registering(Exec::class) {
  workingDir(rootProject.layout.projectDirectory.asFile.path)

  commandLine(
    "make",
    "-C", "third_party",
    "RELEASE=yes",  // third-party natives are always built in release mode
    "NATIVE=$buildForNative",
    "-j$jobs",
  )

  outputs.cacheIf { true }
  outputs.dir(targetSqliteDir.asFile.path)
}

private fun TargetInfo.matches(criteria: TargetPredicate): Boolean =
  TargetCriteria.allOf(this, criteria)

val isClang19 = findProperty("elide.compiler") == "clang-19"

fun resolveCargoConfig(target: TargetInfo): File? = when {
  target.matches(Criteria.MacAmd64) -> "macos-x86_64"
  target.matches(Criteria.MacArm64) -> "macos-arm64"
  target.matches(Criteria.Amd64) -> if (isClang19) "clang-x86_64" else "x86_64"
  target.matches(Criteria.Arm64) -> if (isClang19) "clang-arm64" else "arm64"
  else -> null
}?.let { name ->
  rootProject.layout.projectDirectory.file(".cargo/config.$name.toml").asFile.let {
    if (!it.exists()) error("No such Cargo configuration: '.cargo/config.$name.toml'")
    it
  }
}

val targetInfo = TargetInfo.current(project)
val targetDir: String =
  rootProject.layout.projectDirectory.dir("target/${targetInfo.triple}/$nativesType").asFile.absolutePath

val rootDir: String = rootProject.layout.projectDirectory.asFile.path
val cargoConfig: String? = resolveCargoConfig(elideTarget)?.absolutePath

afterEvaluate {
  logger.lifecycle(
    "Compiling natives for target '${elideTarget.triple}' " +
    "(config: ${resolveCargoConfig(elideTarget)?.name ?: "default"})"
  )
}

tasks.nativeTest.configure {
  runtimeArgs.add(javaLibPath.get().toString().let {
      "-Djava.library.path=$it"
  })
  runtimeArgs.addAll(listOf(
    "-Delide.test=true",
    "-Delide.nativeTest=true",
  ))
}

val baseCargoFlags = listOfNotNull(
  "--color=always",
  "build",
  "--target",
  targetInfo.triple,
).plus(
  cargoConfig?.let {
    listOf("--config", it)
  } ?: emptyList()
)

val buildRustNativesForHostRelease by tasks.registering(Exec::class) {
  workingDir(rootDir)
  dependsOn("buildThirdPartyNatives")

  executable = "cargo"
  args(baseCargoFlags.plus("--release"))
  environment("JAVA_HOME", System.getProperty("java.home"))
  environment("MACOSX_DEPLOYMENT_TARGET", "14.0")

  outputs.upToDateWhen { true }
  outputs.dir(targetDir)
}

val buildRustNativesForHostDebug by tasks.registering(Exec::class) {
  workingDir(rootDir)
  dependsOn("buildThirdPartyNatives")

  executable = "cargo"
  args(baseCargoFlags)
  environment("JAVA_HOME", System.getProperty("java.home"))
  environment("MACOSX_DEPLOYMENT_TARGET", "14.0")

  outputs.upToDateWhen { true }
  outputs.dir(targetDir)
}

val buildRustNativesForHost by tasks.registering(Exec::class) {
  workingDir(rootDir)
  dependsOn("buildThirdPartyNatives")

  executable = "cargo"
  args(baseCargoFlags.plus(listOfNotNull(if (isRelease) "--release" else null)))
  environment("JAVA_HOME", System.getProperty("java.home"))
  environment("MACOSX_DEPLOYMENT_TARGET", "14.0")

  outputs.upToDateWhen { true }
  outputs.dir(targetDir)
}

val selectedRustNatives: String = if (isRelease)
  buildRustNativesForHostRelease.name
else
  buildRustNativesForHost.name

tasks.nativeTestCompile.configure {
  doFirst {
    mkdir(layout.buildDirectory.dir("classes/java/test"))
  }
}

val natives by tasks.registering {
  group = "build"
  description = "Build natives via Make and Cargo"
  dependsOn(buildThirdPartyNatives.name, selectedRustNatives)
}

listOf(
  tasks.build,
  tasks.check,
  tasks.test,
).forEach {
  it.configure {
    dependsOn(natives)
  }
}
