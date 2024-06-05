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
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import com.jakewharton.mosaic.gradle.MosaicExtension
import io.micronaut.gradle.MicronautRuntime
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Files
import java.nio.file.Path
import elide.internal.conventions.kotlin.KotlinTarget

plugins {
  java
  `java-library`
  publishing
  jacoco
  `jvm-test-suite`
  `maven-publish`

  kotlin("jvm")
  kotlin("kapt")
  // kotlin("plugin.compose")
  kotlin("plugin.serialization")
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.micronaut.minimal.application)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.elide.conventions)
}

buildscript {
  repositories {
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
        includeGroup("com.jakewharton.mosaic")
      }
    }
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    google()
  }
  dependencies {
    classpath(libs.plugin.mosaic)
  }
}

// Flags affecting this build script:
//
// - `elide.release`: true/false
// - `elide.buildMode`: `dev`, `release`, `debug`
// - `elide.targetOs`: `darwin`, `linux`, `windows`
// - `elide.targetArch`: `amd64`, `arm64`

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

val isRelease = !quickbuild && (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)

val isDebug = !isRelease && (
  project.properties["elide.buildMode"] == "debug"
)

val hostIsLinux = HostManager.hostIsLinux
val hostIsMac = HostManager.hostIsMac
val hostIsWindows = HostManager.hostIsMingw
val nativesType = if (isRelease) "release" else "debug"
val nativeTargetType = if (isRelease) "nativeOptimizedCompile" else "nativeCompile"
val entrypoint = "elide.tool.cli.ElideKt"

val enablePkl = false
val enableWasm = true
val enablePython = true
val enableRuby = false
val enableLlvm = false
val enableJvm = hostIsLinux
val enableKotlin = false
val enableSqlite = true
val enableSqliteStatic = false
val enableStaticJni = true
val enableToolchains = !hostIsLinux
val enableFfm = hostIsLinux && System.getProperty("os.arch") == "amd64"
val oracleGvm = false
val enableEdge = true
val enableStage = false
val enableNativeTransportV2 = true
val enableDynamicPlugins = false
val enableDeprecated = false
val enableJit = true
val enablePreinitializeAll = false
val enableTools = true
val enableProguard = false
val enableExperimental = false
val enableEmbeddedResources = false
val enableResourceFilter = false
val enableAuxCache = false
val enableJpms = false
val enableEmbeddedBuilder = false
val enableBuildReport = true
val enableG1 = false
val enablePgo = false
val enablePgoSampling = false
val enablePgoInstrumentation = false
val enablePgoReport = true
val enableJna = false
val enableSbom = oracleGvm
val enableSbomStrict = false
val glibcTarget = "glibc"
val dumpPointsTo = false

val exclusions = listOfNotNull(
  // always exclude non-jpms jna
  libs.jna.asProvider(),

  // prefer jpms jna when enabled
  if (enableJna) null else libs.jna.jpms,

  // only include jline jni integration if ffm is disabled
  if (!enableFfm) null else libs.jline.terminal.jni,

  // only include jline jna integration if jna is enabled and ffm is disabled
  if (!enableFfm && enableJna) null else libs.jline.terminal.jna,

  // exclude kotlin compiler if kotlin is not enabled; it includes shadowed jline configs
  if (enableKotlin) null else libs.kotlin.compiler.embedded,
).plus(listOf(
  // disable netty native transports if our own transport libraries are in use
  libs.netty.transport.native.epoll,
  libs.netty.transport.native.kqueue,
).onlyIf(enableNativeTransportV2))

// Java Launcher (GraalVM at either EA or LTS)
val edgeJvmTarget = 23
val ltsJvmTarget = 21
val edgeJvm = JavaVersion.toVersion(edgeJvmTarget)
val ltsJvm = JavaVersion.toVersion(ltsJvmTarget)
val selectedJvmTarget = if (enableEdge) edgeJvmTarget else ltsJvmTarget
val selectedJvm = if (enableEdge) edgeJvm else ltsJvm

val jvmType: JvmVendorSpec =
  if (oracleGvm) JvmVendorSpec.matching("Oracle Corporation") else JvmVendorSpec.GRAAL_VM

val gvmLauncher = javaToolchains.launcherFor {
  languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
  vendor.set(jvmType)
}

private fun namedConfig(name: String, type: String = "resource"): String =
  layout.projectDirectory.dir("src/config").file("$type-config-$name.json").asFile.path.toString()

private fun platformConfig(type: String = "resource"): String {
  val arch = (properties["elide.targetArch"]?.toString() ?: System.getProperty("os.arch")).let {
    if (it.contains("x86")) "amd64" else it
  }
  return when {
    hostIsLinux -> namedConfig("linux-$arch", type = type)
    hostIsMac -> namedConfig("darwin-$arch", type = type)
    hostIsWindows -> namedConfig("windows-$arch", type = type)
    else -> error("Unsupported platform for '$type' configuration")
  }
}

val nativesRootTemplate: (String) -> String = { version ->
  "/tmp/elide-runtime/v$version/native"
}

val jvmOnlyCompileArgs: List<String> = listOfNotNull(
  // Nothing at this time.
)

val jvmCompileArgs = listOfNotNull(
  "--enable-preview",
  "--add-modules=jdk.incubator.vector",
  "--enable-native-access=" + listOfNotNull(
    "ALL-UNNAMED",
  ).joinToString(","),
  "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
).plus(if (enableJpms) listOf(
  "--add-reads=elide.cli=ALL-UNNAMED",
  "--add-reads=elide.graalvm=ALL-UNNAMED",
) else emptyList()).plus(if (enableEmbeddedBuilder) listOf(
  "--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
) else emptyList())

val jvmRuntimeArgs = emptyList<String>()

val nativeCompileJvmArgs = jvmCompileArgs.map {
  "-J$it"
}

val jvmModuleArgs = listOf(
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
).plus(jvmCompileArgs).plus(jvmRuntimeArgs)

val ktCompilerArgs = mutableListOf(
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",

  // opt-in to Elide's delicate runtime API
  "-opt-in=elide.runtime.core.DelicateElideApi",

  // Fix: Suppress Kotlin version compatibility check for Compose plugin (applied by Mosaic).
  // Note: Re-enable this if the Kotlin version differs from what Compose/Mosaic expects.
  "-P=plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=2.0.0",
)

elide {
  kotlin {
    powerAssert = true
    atomicFu = true
    target = KotlinTarget.JVM
    customKotlinCompilerArgs = ktCompilerArgs
  }

  jvm {
    target = JVM_21
  }

  java {
    configureModularity = false
    includeJavadoc = false
    includeSources = false
  }

  checks {
    spotless = false
  }

  docs {
    enabled = false
  }
}

java {
  sourceCompatibility = selectedJvm
  targetCompatibility = selectedJvm
  if (enableJpms) modularity.inferModulePath = true

  toolchain {
    languageVersion.set(JavaLanguageVersion.of(selectedJvmTarget))
    vendor.set(jvmType)
  }
}

kapt {
  useBuildCache = true
  includeCompileClasspath = false
  strictMode = true
  correctErrorTypes = true
}

kotlin {
  target.compilations.all {
    compilerOptions {
      allWarningsAsErrors = true
      freeCompilerArgs.set(freeCompilerArgs.get().plus(ktCompilerArgs).toSortedSet().toList())
    }
  }
}

sourceSets {
  val main by getting {
    if (enableJpms) java.srcDirs(
      layout.projectDirectory.dir("src/main/java9"),
    )
  }
}

apply(plugin = "com.jakewharton.mosaic")
the<MosaicExtension>().kotlinCompilerPlugin = libs.androidx.compose.compiler.get().toString()

val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false
val cliVersion = if (stamp) {
  libs.versions.elide.asProvider().get()
} else {
  "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
}

val languagePluginPaths = if (!enableDynamicPlugins) emptyList() else listOf(
  "graalvm-py",
  "graalvm-rb",
).map { module ->
  project(":packages:$module").layout.buildDirectory.dir("native/nativeSharedCompile").get().asFile.path
}

val nativesPath = nativesRootTemplate(cliVersion)
val umbrellaNativesPath: String = rootProject.layout.projectDirectory.dir("target/$nativesType").asFile.path
val gvmResourcesPath: String = layout.buildDirectory.dir("native/nativeCompile/resources")
  .get()
  .asFile
  .path

buildConfig {
  className("ElideCLITool")
  packageName("elide.tool.cli.cfg")
  useKotlinOutput()
  buildConfigField("String", "ELIDE_RELEASE_TYPE", if (isRelease) "\"RELEASE\"" else "\"DEV\"")
  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"$cliVersion\"")
  buildConfigField("String", "NATIVES_PATH", "\"${nativesPath}\"")
  buildConfigField("String", "GVM_RESOURCES", "\"${gvmResourcesPath}\"")
}

val modules: Configuration by configurations.creating

val classpathExtras: Configuration by configurations.creating {
  extendsFrom(configurations.runtimeClasspath.get())
}

val jvmOnly: Configuration by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

dependencies {
  kapt(mn.micronaut.inject.java)
  kapt(libs.picocli.codegen)
  classpathExtras(mn.micronaut.core.processor)

  api(libs.clikt)
  api(libs.picocli)
  api(libs.guava)
  api(projects.packages.base)
  api(libs.snakeyaml)
  api(mn.micronaut.inject)
  implementation(libs.jansi)
  implementation(libs.picocli.jansi.graalvm)
  implementation(mn.micronaut.picocli)
  implementation(projects.packages.cliBridge)
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.logback)
  implementation(libs.bouncycastle)
  implementation(libs.mosaic)
  runtimeOnly(mn.micronaut.runtime)

  implementation(libs.jline.reader)
  implementation(libs.jline.console)
  implementation(libs.jline.terminal.core)
  implementation(libs.jline.terminal.jansi)
  implementation(libs.jline.terminal.jni)
  implementation(libs.jline.terminal.ffm)
  implementation(libs.jline.builtins)
  implementation(libs.jline.graal) {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
  }

  // SQLite Engine
  if (enableSqlite) {
    implementation(projects.packages.sqlite)
    testImplementation(libs.sqlite)
  }

  // GraalVM: Engines
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmTs)
  implementation(projects.packages.graalvmWasm)
  api(libs.graalvm.polyglot)
  api(libs.graalvm.js.language)
  compileOnly(libs.graalvm.svm)
  if (oracleGvm) compileOnly(libs.graalvm.truffle.enterprise)

  // GraalVM: Dynamic Language Engines
  if (enableDynamicPlugins) {
    compileOnly(projects.packages.graalvmRb)
    compileOnly(projects.packages.graalvmPy)
  } else {
    if (enableRuby) implementation(projects.packages.graalvmRb)
    if (enablePython) implementation(projects.packages.graalvmPy)
    if (enableLlvm) implementation(projects.packages.graalvmLlvm)
    if (enableJvm) {
      implementation(projects.packages.graalvmJvm)
      implementation(projects.packages.graalvmJava)
      if (enableKotlin) implementation(projects.packages.graalvmKt)
    }
  }

  // GraalVM: Tooling
  implementation(libs.graalvm.tools.dap)
  implementation(libs.graalvm.tools.chromeinspector)
  implementation(libs.graalvm.tools.profiler)
  implementation(libs.graalvm.tools.coverage)

  // KotlinX
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)

  // Logging
  api(libs.slf4j)
  api(libs.slf4j.jul)
  api(libs.slf4j.log4j.bridge)

  // Console UI
  implementation(libs.magicProgress)

  runtimeOnly(mn.micronaut.graal)
  implementation(mn.netty.handler)
  implementation(libs.netty.tcnative)

  // JVM-only dependencies which are filtered for native builds.
  if (!enableJna) {
    jvmOnly(libs.jna.jpms)
  } else {
    implementation(libs.jna.jpms)
  }

  // Tests
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(mn.micronaut.test.junit5)

  implementation(libs.netty.tcnative.boringssl.static)
  implementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-x86_64") })
  implementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-aarch_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-x86_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-aarch_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })

  if (enableNativeTransportV2) {
    implementation(projects.packages.transport.transportEpoll)
    implementation(projects.packages.transport.transportKqueue)
    implementation(libs.netty.transport.native.classes.epoll)
    implementation(libs.netty.transport.native.classes.kqueue)
    implementation(libs.netty.transport.native.classes.iouring)
  } else {
    implementation(libs.netty.transport.native.epoll)
    implementation(libs.netty.transport.native.unix)
    implementation(libs.netty.transport.native.kqueue)
    implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-x86_64") })
    implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-aarch_64") })
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-aarch_64") })

    implementation(libs.netty.transport.native.iouring)
    implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-aarch_64") })
  }

  if (oracleGvm && !enableEdge) {
    if (enableExperimental && hostIsLinux) {
      api(libs.graalvm.truffle.nfi.panama)
    }
    implementation(libs.graalvm.truffle.nfi.native.darwin.aarch64)
    implementation(libs.graalvm.truffle.nfi.native.darwin.amd64)
    implementation(libs.graalvm.truffle.nfi.native.linux.aarch64)
    implementation(libs.graalvm.truffle.nfi.native.linux.amd64)
  }
}

application {
  mainClass = entrypoint
  if (enableJpms) mainModule = "elide.cli"
}

val targetOs = when {
  Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
  Os.isFamily(Os.FAMILY_MAC) -> "darwin"
  Os.isFamily(Os.FAMILY_UNIX) -> "linux"
  else -> "generic"
}

val targetArch: String = System.getProperty("os.arch", "unknown")
val targetTag = "$targetOs-$targetArch"

signing {
  isRequired = properties["enableSigning"] == "true"
}

/**
 * Framework: Micronaut
 */

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = MicronautRuntime.NONE
  enableNativeImage(true)

  processing {
    incremental = true
    annotations.addAll(
      listOf(
        "elide.tool.cli.*",
      ),
    )
  }

  aot {
    configFile = file("$projectDir/aot-native.properties")
    this@aot.version = libs.versions.micronaut.aot.get()

    convertYamlToJava = true
    precomputeOperations = true
    cacheEnvironment = true
    deduceEnvironment = true
    replaceLogbackXml = true

    optimizeServiceLoading = true
    optimizeClassLoading = true
    optimizeNetty = true
    possibleEnvironments = listOf("cli", "cloud", "gcp", "aws", "azure")

    netty {
      enabled = true
      machineId = "13-37-7C-D1-6F-F5"
      pid = "1337"
    }
  }
}

tasks.withType(Test::class).configureEach {
  useJUnitPlatform()
  systemProperty("elide.test", "true")
  maxHeapSize = "1g"
}

private fun <T> onlyIf(flag: Boolean, value: T): T? = value.takeIf { flag }
private fun <T> List<T>.onlyIf(flag: Boolean): List<T> = if (flag) this else emptyList()


/**
 * --------- Build: CLI Native Image
 */

val commonGvmArgs = listOfNotNull(
  "-H:+UseCompressedReferences",
  onlyIf(enableBuildReport, "-H:+BuildReport"),
).onlyIf(oracleGvm)

val nativeImageBuildDebug = properties["nativeImageBuildDebug"] == "true"
val nativeImageBuildVerbose = properties["nativeImageBuildVerbose"] == "true"

val stagedNativeArgs: List<String> = listOfNotNull(
  "-H:+LayeredBaseImageAnalysis",
  "-H:+RemoveUnusedSymbols",

  onlyIf(oracleGvm, "-H:ReservedAuxiliaryImageBytes=${1024 * 1024}"),

  onlyIf(enableExperimental, "-H:+ProfileCompiledMethods"),
  onlyIf(enableExperimental, "-H:+ProfileConstantObjects"),
  onlyIf(enableExperimental, "-H:+ProfileLockElimination"),
  onlyIf(enableExperimental, "-H:+ProfileMonitors"),
  onlyIf(enableExperimental, "-H:+ProfileOptBulkAllocation"),
  onlyIf(enableExperimental, "-H:+SIMDVectorizationDirectLoadStore"),
  onlyIf(enableExperimental, "-H:+SIMDVectorizationSingletons"),
  onlyIf(enableExperimental, "-H:+VectorPolynomialIntrinsics"),
  onlyIf(enableExperimental, "-H:-IncludeMethodData"),
  onlyIf(enableExperimental, "-H:-IncludeNodeSourcePositions"),
  onlyIf(enableExperimental, "-H:InlineBeforeAnalysisAllowedInlinings=1000"),
  onlyIf(enableExperimental, "-H:InlineBeforeAnalysisMethodHandleAllowedInlinings=1000"),
  onlyIf(enableExperimental, "-H:InlineBeforeAnalysisMethodHandleAllowedNodes=1000"),
  onlyIf(enableExperimental, "-H:InlinedCompilerNodeLimit=10000"),

  // Breakages
//  "-H:+LSRAOptimization",
//  "-H:+LIRProfileMethods",
//  "-H:+LIRProfileMoves",
//  "-H:+UseExperimentalReachabilityAnalysis",  // not supported by truffle feature
//  "-H:+UseReachabilityMethodSummaries",  // not supported by truffle feature
//  "-H:+VMContinuations",  // not supported with runtime compilation
)

val deprecatedNativeArgs = listOf(
  "--configure-reflection-metadata",
  "--enable-all-security-services",
)

val enabledFeatures = listOfNotNull(
  "elide.tool.feature.ToolingUmbrellaFeature",
  onlyIf(enableSqlite, "elide.runtime.feature.engine.NativeSQLiteFeature"),
)

val linkerOptions: List<String> = listOfNotNull()

val commonNativeArgs = listOfNotNull(
  // Debugging flags:
  // "--verbose",
  // "-H:AbortOnFieldReachable=com.sun.jna.internal.Cleaner${'$'}CleanerThread.this${'$'}0",
  // "--trace-object-instantiation=com.sun.jna.internal.Cleaner",
  onlyIf(isDebug, "-H:+JNIVerboseLookupErrors"),
  onlyIf(!enableJit, "-J-Dtruffle.TruffleRuntime=com.oracle.truffle.api.impl.DefaultTruffleRuntime"),
  onlyIf(enableFfm, "-H:+ForeignAPISupport"),
  onlyIf(dumpPointsTo, "-H:PrintAnalysisCallTreeType=CSV"),
  onlyIf(dumpPointsTo, "-H:+PrintImageObjectTree"),
  onlyIf(enabledFeatures.isNotEmpty(), "--features=${enabledFeatures.joinToString(",")}"),
  // Flags which should be dropped after fixes (Mosaic crash):
  "--report-unsupported-elements-at-runtime",
  // Common flags:
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  "--enable-url-protocols=jar",
  "--macro:truffle-svm",
  "--enable-native-access=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  "-H:+ReportExceptionStackTraces",
  "-H:+JNIEnhancedErrorCodes",
  "-H:+AddAllCharsets",
  "-H:MaxRuntimeCompileMethods=20000",
  "-H:+ParseRuntimeOptions",
  "-H:NativeLinkerOption=-L$nativesPath",
  "-Delide.staticJni=$enableStaticJni",
  "-J-Delide.staticJni=$enableStaticJni",
  "-Delide.natives=$nativesPath",
  "-J-Delide.natives=$nativesPath",
  "-Dorg.sqlite.lib.path=$nativesPath",
  "-J-Dorg.sqlite.lib.path=$nativesPath",
  "-Dorg.sqlite.lib.exportPath=$nativesPath",
  "-J-Dorg.sqlite.lib.exportPath=$nativesPath",
  "-Dio.netty.native.workdir=$nativesPath",
  "-J-Dio.netty.native.workdir=$nativesPath",
  "-Dio.netty.native.deleteLibAfterLoading=false",
  "-J-Dio.netty.native.deleteLibAfterLoading=false",
  "-J-Dio.netty.allocator.type=unpooled",
  "-R:MaxDirectMemorySize=256M",
  "-Delide.nativeTransport.v2=${enableNativeTransportV2}",
  "-J-Delide.nativeTransport.v2=${enableNativeTransportV2}",
  "-J-Dtruffle.TrustAllTruffleRuntimeProviders=true",
  "-J-Dgraalvm.locatorDisabled=false",
  "-J-Dpolyglotimpl.DisableVersionChecks=true",
  "-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true",
  "-J-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true",
  "-Dpolyglot.image-build-time.PreinitializeContexts=${preinitializedContexts.joinToString(",")}",
  "-J-Dpolyglot.image-build-time.PreinitializeContexts=${preinitializedContexts.joinToString(",")}",
  onlyIf(enablePgoInstrumentation, "--pgo-instrument"),
  onlyIf(enablePgoSampling, "--pgo-sampling"),
  onlyIf(enablePgoInstrumentation && enablePgo, "-H:+BuildReportSamplerFlamegraph"),
).asSequence().plus(
  languagePluginPaths.plus(listOf(nativesPath, umbrellaNativesPath)).filter {
    Files.exists(Path.of(it))
  }.map {
    "-H:CLibraryPath=$it"
  }
).plus(
  listOf("-H:+UnlockExperimentalVMOptions")
).plus(
  commonGvmArgs.onlyIf(oracleGvm)
).plus(
  listOf("--debug-attach").onlyIf(nativeImageBuildDebug)
).plus(
  linkerOptions
).toList()

val debugFlags: List<String> = listOfNotNull(
  "--verbose",
  "-march=compatibility",
  "-H:+SourceLevelDebug",
  "-H:-DeleteLocalSymbols",
  "-H:-RemoveUnusedSymbols",
  "-H:+PreserveFramePointer",
  // "-J-Xlog:library=info",
  // "-H:-ReduceDCE",
  // "-H:+PrintMethodHistogram",
  // "-H:+PrintPointsToStatistics",
  // "-H:+PrintRuntimeCompileMethods",
  // "-H:+ReportPerformedSubstitutions",
).plus(
  listOf("-g").onlyIf(hostIsLinux)
)

val experimentalFlags = listOf(
  "-H:+SupportContinuations",  // -H:+SupportContinuations is in use, but is not supported together with Truffle JIT compilation
  "-H:+UseStringInlining",  // String inlining optimization is not supported when just-in-time compilation is used

  // Not enabled for regular builds yet
  "-H:±UseExperimentalReachabilityAnalysis",
  "-H:±UseNewExperimentalClassInitialization",
  "-H:±UseDedicatedVMOperationThread",
  "-H:±SupportCompileInIsolates",

  "-R:±AlwaysInlineIntrinsics",
  "-R:±AlwaysInlineVTableStubs",

  // Profiling
  "-R:±ProfileMonitors",
  "-R:±ProfileOptBulkAllocation",
  "-R:±ProfileCompiledMethods",
  "-R:±ProfileConstantObjects",
  "-R:±ProfileLockElimination",

  // Crashes
  "-H:+ProtectionKeys",
  "-H:+UseThinLocking",

  // Significant slowdowns
  "-H:+RunMainInNewThread",

  // Unclear stability
  "-H:+LSRAOptimization",
  "-H:+VectorPolynomialIntrinsics",
)

// CFlags for release mode.
val releaseCFlags: List<String> = listOf()

// PGO profiles to specify in release mode.
val profiles: List<String> = listOf(
  "js-repl.iprof",
  "js-serve.iprof",
  "py-repl.iprof",
  "python.iprof",
  "ruby-repl.iprof",
  "ruby.iprof",
  "selftest.iprof"
)

// GVM release flags
val gvmReleaseFlags: List<String> = listOf(
  "-O4",
)

// Experimental C-compiler flags.
val experimentalCFlags: List<String> = listOf(
  "-flto=thin",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
  "-O4",
  "-H:+LocalizationOptimizedMode",
  "-H:+RemoveUnusedSymbols",
).asSequence().plus(releaseCFlags.plus(if (enableExperimental) experimentalCFlags else emptyList()).flatMap {
  listOf(
    "-H:NativeLinkerOption=$it",
    "--native-compiler-options=$it",
  )
}).plus(
  listOf("--pgo=${profiles.joinToString(",")}").onlyIf(enablePgo)
).plus(listOf(
  if (oracleGvm && enableSbom) listOf(
    if (enableSbomStrict) "--enable-sbom=cyclonedx,export,strict" else "--enable-sbom=cyclonedx,export"
  ) else emptyList(),
  if (oracleGvm) gvmReleaseFlags else emptyList(),
).flatten()).toList()

val jvmDefs = mapOf(
  "elide.natives" to nativesPath,
  "elide.nativeTransport.v2" to enableNativeTransportV2.toString(),
  "io.netty.allocator.type" to "unpooled",
  "io.netty.native.deleteLibAfterLoading" to "false",
  "io.netty.native.detectNativeLibraryDuplicates" to "false",
  "io.netty.native.tryPatchShadedId" to "false",
  "java.net.preferIPv4Stack" to "true",
  "logback.statusListenerClass" to "ch.qos.logback.core.status.NopStatusListener",
  "networkaddress.cache.ttl" to "10",
  "polyglotimpl.DisableVersionChecks" to "true",
  "user.country" to "US",
  "user.language" to "en",
  "org.sqlite.lib.path" to nativesPath,
  "org.sqlite.lib.exportPath" to nativesPath,
  "io.netty.native.workdir" to nativesPath,
  "io.netty.native.deleteLibAfterLoading" to false.toString(),
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
)

val initializeAtRuntime: List<String> = listOfNotNull(
  onlyIf(!enableSqliteStatic, "org.sqlite.SQLiteJDBCLoader"),
  onlyIf(!enableSqliteStatic, "org.sqlite.core.NativeDB"),
  onlyIf(enableNativeTransportV2, "io.netty.channel.kqueue.Native"),

  "java.awt.Desktop",
  "java.awt.Toolkit",
  "sun.awt.X11.MotifDnDConstants",
  "sun.awt.X11.WindowPropertyGetter",
  "sun.awt.X11.XBaseWindow",
  "sun.awt.X11.XDataTransferer",
  "sun.awt.X11.XDnDConstants",
  "sun.awt.X11.XDragAndDropProtocols",
  "sun.awt.X11.XRootWindow",
  "sun.awt.X11.XRootWindow${'$'}LazyHolder",
  "sun.awt.X11.XSelection",
  "sun.awt.X11.XSystemTrayPeer",
  "sun.awt.X11.XToolkit",
  "sun.awt.X11.XToolkitThreadBlockedHandler",
  "sun.awt.X11.XWM",
  "sun.awt.X11.XWindow",
  "sun.awt.X11GraphicsConfig",
  "sun.awt.dnd.SunDropTargetContextPeer${'$'}EventDispatcher",
  "sun.font.PhysicalStrike",
  "sun.font.StrikeCache",
  "sun.font.SunFontManager",
  "sun.java2d.Disposer",
  "sun.nio.ch.UnixDomainSockets",
  "sun.security.provider.NativePRNG",

  // --- JNA -----

  "com.sun.jna.Structure${'$'}FFIType",
  "com.sun.jna.platform.mac.IOKit",
  "com.sun.jna.platform.mac.IOKitUtil",
  "com.sun.jna.platform.mac.SystemB",

  // --- JLine -----

  "org.jline.terminal.impl.jna.osx.OsXNativePty",
  "org.jline.terminal.impl.jna.linux.LinuxNativePty",

  // --- JVM/JDK -----

  "com.sun.tools.javac.file.Locations",

  // --- OSHI -----

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

  // --- Logback -----

  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",

  // --- Micronaut -----

  "io.micronaut.core.util.KotlinUtils",
  "io.micronaut.core.io.socket.SocketUtils",
  "io.micronaut.core.type.RuntimeTypeInformation${'$'}LazyTypeInfo",
  "io.micronaut.context.env.CachedEnvironment",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver",
  "io.micronaut.context.env.exp.RandomPropertyExpressionResolver${'$'}LazyInit",

  // --- Kotlin -----

  "kotlin.random.AbstractPlatformRandom",
  "kotlin.random.XorWowRandom",
  "kotlin.random.Random",
  "kotlin.random.Random${'$'}Default",
  "kotlin.random.RandomKt",
  "kotlin.random.jdk8.PlatformThreadLocalRandom",

  // --- Netty -----

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
  "io.netty.handler.ssl.ReferenceCountedOpenSslEngine",
  "io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  "io.netty.incubator.channel.uring.Native",
  "io.netty.handler.codec.http.HttpObjectEncoder",

  // --- Netty: Native Crypto -----

  "io.netty.internal.tcnative.Buffer",
  "io.netty.internal.tcnative.CertificateVerifier",
  "io.netty.internal.tcnative.Library",
  "io.netty.internal.tcnative.SSL",
  "io.netty.internal.tcnative.SSLContext",
  "io.netty.internal.tcnative.SSLSession",

  // --- BouncyCastle -----

  "org.bouncycastle.jcajce.provider.drbg.DRBG",
  "org.bouncycastle.jcajce.provider.drbg.DRBG${'$'}Default",

  // --- Mosaic -----

  "com.jakewharton.mosaic.PlatformKt",

  // --- JLine -----

  "org.jline.nativ.Kernel32",
  "org.jline.nativ.Kernel32${'$'}CHAR_INFO",
  "org.jline.nativ.Kernel32${'$'}CONSOLE_SCREEN_BUFFER_INFO",
  "org.jline.nativ.Kernel32${'$'}COORD",
  "org.jline.nativ.Kernel32${'$'}FOCUS_EVENT_RECORD",
  "org.jline.nativ.Kernel32${'$'}INPUT_EVENT_RECORD",
  "org.jline.nativ.Kernel32${'$'}INPUT_RECORD",
  "org.jline.nativ.Kernel32${'$'}KEY_EVENT_RECORD",
  "org.jline.nativ.Kernel32${'$'}MENU_EVENT_RECORD",
  "org.jline.nativ.Kernel32${'$'}MOUSE_EVENT_RECORD",
  "org.jline.nativ.Kernel32${'$'}SMALL_RECT",
  "org.jline.nativ.Kernel32${'$'}WINDOW_BUFFER_SIZE_RECORD",

  // --- Elide -----

  "elide.runtime.intrinsics.server.http.netty.NettyRequestHandler",
  "elide.runtime.intrinsics.server.http.netty.NettyHttpResponse",
  "elide.runtime.intrinsics.server.http.netty.IOUringTransport",
  "elide.runtime.gvm.internals.node.process.NodeProcess${'$'}NodeProcessModuleImpl",

  // --- Elide CLI -----

  "elide.tool.cli.cmd.help.HelpCommand",
  "elide.tool.cli.cmd.discord",
  "elide.tool.cli.cmd.discord.ToolDiscordCommand",
  "elide.tool.cli.cmd.selftest.SelfTestCommand",
)

val pklArgs: List<String> = listOf(
  "-H:IncludeResources=org/pkl/core/stdlib/.*\\.pkl",
  "-H:IncludeResources=org/jline/utils/.*",
  "-H:IncludeResources=org/pkl/commons/cli/commands/IncludedCARoots.pem",
  "-H:IncludeResourceBundles=org.pkl.core.errorMessages",
)

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntimeTest: List<String> = emptyList()

val defaultPlatformArgs: List<String> = listOf()

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=compatibility",
  "--gc=serial",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (oracleGvm) listOf(
  "-Delide.vm.engine.preinitialize=true",
) else listOf(
  "-Delide.vm.engine.preinitialize=false",
)).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())).plus(if (oracleGvm && enableAuxCache) listOf(
  "-H:-AuxiliaryEngineCache",
) else emptyList())

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=compatibility",
  "--gc=serial",
  "-R:MaximumHeapSizePercent=80",
).plus(if (oracleGvm) listOf(
  "-Delide.vm.engine.preinitialize=true",
) else listOf(
  "-Delide.vm.engine.preinitialize=false",
)).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx48g",
))).plus(if (oracleGvm && enableAuxCache) listOf(
  "-H:+AuxiliaryEngineCache",
) else emptyList())

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.toList()

val linuxOnlyArgs = defaultPlatformArgs.plus(
  listOf(
    "-march=compatibility",
    "-H:+StaticExecutableWithDynamicLibC",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
  ),
).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:+UseG1GC",
    "-H:-AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=false",
  ) else listOf(
    "--gc=serial",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  ).plus(if (oracleGvm && enableAuxCache) listOf(
    "-H:+AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=true",
  ) else emptyList())
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx24g",
))

val linuxGvmReleaseFlags = listOf(
  "-H:+ObjectInlining",
)

val linuxReleaseArgs = linuxOnlyArgs.plus(
  listOf(
    "-R:+WriteableCodeCache",
    "-H:+StripDebugInfo",
  ).plus(if (oracleGvm) linuxGvmReleaseFlags else emptyList()),
)

val muslArgs = listOf(
  "--static",
  "--libc=musl",
)

val testOnlyArgs: List<String> = emptyList()

val nativeOverrideArgs: List<String> = listOf(
  "--exclude-config", "python-language-${libs.versions.graalvm.pin.get()}.jar", "META-INF\\/native-image\\/.*.properties",
)

fun nativeCliImageArgs(
  platform: String = "generic",
  target: String = glibcTarget,
  debug: Boolean = isDebug,
  test: Boolean = false,
  release: Boolean = isRelease,
): List<String> =
  commonNativeArgs.asSequence()
    .plus(deprecatedNativeArgs.onlyIf(enableDeprecated))
    .plus(stagedNativeArgs.onlyIf(enableStage))
    .plus(jvmCompileArgs)
    .plus(pklArgs.onlyIf(enablePkl))
    .plus(StringBuilder().apply {
      append(rootProject.layout.projectDirectory.dir("target/$nativesType").asFile.path)
      append(File.pathSeparator)
      append(nativesPath)
      append(File.pathSeparator)
      languagePluginPaths.forEach {
        append(it)
        append(File.pathSeparator)
      }
      append(System.getProperty("java.library.path", ""))
    }.toString().let {
      listOf(
        "-Djava.library.path=$it",
        "-J-Djava.library.path=$it",
      )
    }
  ).plus(
    jvmCompileArgs.map { "-J$it" },
  ).plus(listOf(
      "--initialize-at-build-time="
  )).plus(
    initializeAtRuntime.map { "--initialize-at-run-time=$it" }
  ).plus(
    when (platform) {
      "windows" -> if (release) windowsReleaseArgs else windowsOnlyArgs
      "darwin" -> if (release) darwinReleaseArgs else darwinOnlyArgs
      "linux" -> if (target == "musl") muslArgs else (if (release) linuxReleaseArgs else linuxOnlyArgs)
      else -> defaultPlatformArgs
    },
  ).plus(
    testOnlyArgs.plus(
      initializeAtRuntimeTest.map {
        "--initialize-at-run-time=$it"
      },
    ).plus(
      rerunAtRuntimeTest.map {
        "--rerun-class-initialization-at-runtime=$it"
      },
    ).onlyIf(test),
  ).plus(
    jvmDefs.map { "-D${it.key}=${it.value}" },
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" },
  ).plus(
    when {
      debug -> debugFlags
      release -> releaseFlags
      else -> emptyList()
    }
  ).plus(
    nativeOverrideArgs
  ).plus(listOf(
    // at the end: resource configuration for this OS and arch. default configuration is already implied.
    "-H:ResourceConfigurationFiles=${platformConfig()}",
  )).toList()

graalvmNative {
  testSupport = true
  toolchainDetection = false

  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    trackReflectionMetadata = true
    enableExperimentalPredefinedClasses = true
    enableExperimentalUnsafeAllocationTracing = true
    enabled = (
      (System.getenv("GRAALVM_AGENT") == "true" || project.properties.containsKey("agent")) &&
      !gradle.startParameter.isProfile  // profiler for build can't also be active
    )

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.addAll(listOf("run", "optimizedRun"))
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      imageName = if (quickbuild) "elide.debug" else "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      buildArgs.addAll(nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs))
      classpath = files(tasks.optimizedNativeJar, configurations.runtimeClasspath)
      if (enableToolchains) javaLauncher = gvmLauncher
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true, platform = targetOs))
      classpath = files(tasks.optimizedNativeJar, configurations.runtimeClasspath)
      if (enableToolchains) javaLauncher = gvmLauncher
    }

    named("test") {
      imageName = "elide.test"
      fallback = false
      quickBuild = true
      if (enableToolchains) javaLauncher = gvmLauncher
      buildArgs.addAll(nativeCliImageArgs(test = true, platform = targetOs).plus(
        nativeCompileJvmArgs
      ))
    }
  }
}

val decompressProfiles: TaskProvider<Copy> by tasks.registering(Copy::class) {
  from(zipTree(layout.projectDirectory.file("profiles.zip")))
  into(layout.buildDirectory.dir("native/nativeOptimizedCompile"))
}

val excludedStatics = arrayOf(
  "about.html",
  "plugin.xml",
)

interface TargetInfo {
  val tag: String

  val resources: List<String>
    get() = listOf(
      "META-INF/elide/embedded/runtime/*/*-$tag.*",
    )
}

enum class ElideTarget(override val tag: String) : TargetInfo {
  MACOS_AARCH64("darwin-aarch64"),
  MACOS_AMD64("darwin-amd64"),
  LINUX_AMD64("linux-amd64"),
  WINDOWS_AMD64("windows-amd64");
}

fun resolveTarget(target: String? = properties["elide.targetOs"] as? String): ElideTarget = when {
  target == "linux-amd64" -> ElideTarget.LINUX_AMD64
  target == "darwin-amd64" -> ElideTarget.MACOS_AMD64
  target == "darwin-aarch64" -> ElideTarget.MACOS_AARCH64
  target == "windows-amd64" -> ElideTarget.WINDOWS_AMD64
  hostIsLinux -> ElideTarget.LINUX_AMD64
  hostIsWindows -> ElideTarget.WINDOWS_AMD64
  hostIsMac -> when (System.getProperty("os.arch")) {
    "aarch64" -> ElideTarget.MACOS_AARCH64
    else -> ElideTarget.MACOS_AMD64
  }

  else -> error("Failed to resolve target platform")
}

fun hostTargetResources(): Array<String> = resolveTarget().resources.toTypedArray()

fun AbstractCopyTask.filterResources(targetArch: String? = null) {
  duplicatesStrategy = EXCLUDE

  exclude(
    "**/*.proto",
    "freebsd/*/*.*",
    "licenses/*.*",
    "misc/registry.properties",
    "linux/arm/*.so",
    "linux/i386/*.so",
    "linux/loongarch64/*.so",
    "linux/mips64/*.so",
    "linux/ppc64/*.so",
    "linux/ppc64le/*.so",
    "linux/s390x/*.so",
    "META-INF/maven",
    "META-INF/maven/*",
    "META-INF/maven/**/*.*",
    "META-INF/plexus/*",
    "META-INF/proguard/*",
    "META-INF/native/freebsd32/*",
    "META-INF/native/freebsd64/*",
    "META-INF/native/linux32/*",
    "META-INF/com.android.tools",
    "META-INF/com.android.tools/*/*",
    *excludedStatics,
    *(if (!enableEmbeddedResources) arrayOf(
      "truffleruby/*/*.rb",
      "META-INF/elide/embedded/runtime/*/*-windows*",
      "META-INF/elide/embedded/runtime/*/*-darwin*",
      "META-INF/elide/embedded/runtime/*/*-linux*",
    ) else emptyArray()),
    *(if (!hostIsWindows) arrayOf(
      "*.dll",
      "**/*.dll",
      "win",
      "win/*",
      "win/*/*",
      "win/**/*.*",
      "win32",
      "win32/*",
      "win32/*/*",
      "win32/**/*.*",
      "Windows",
      "*Windows*",
      "**Windows**",
      "*windows*",
      "**windows**",
      "META-INF/elide/embedded/runtime/*/*-windows*",
    ) else emptyArray()),

    *(if (!hostIsMac) arrayOf(
      "*.dylib",
      "**/*.dylib",
      "darwin",
      "darwin/*",
      "darwin/*/*",
      "darwin/**/*.*",
      "kqueue",
      "*kqueue*",
      "**kqueue**",
      "*/kqueue/*",
      "*/kqueue/**/*.*",
      "META-INF/native/osx/*",
      "META-INF/native/*darwin*",
      "META-INF/native/*osx*",
      "META-INF/native/*macos*",
      "META-INF/native/*.dylib",
      "META-INF/elide/embedded/runtime/*/*-darwin*",
    ) else emptyArray()),

    *(if (!hostIsLinux) arrayOf(
      "*.so",
      "**/*.so",
      "linux/aarch64/*.so",
      "linux/amd64/*.so",
      "epoll",
      "*epoll*",
      "**epoll**",
      "*/epoll/*",
      "*/epoll/**/*.*",
      "uring",
      "*uring*",
      "META-INF/native/linux64/*",
      "META-INF/native/*linux*",
      "META-INF/native/*.so",
      "META-INF/elide/embedded/runtime/*/*-linux*",
    ) else emptyArray()),

    *(when (targetArch ?: System.getProperty("os.arch")) {
      "aarch64" -> arrayOf(
        "*/x86/*",
        "**/x86/**/*.*",
        "*/x86_64/*",
        "*/x86_64/**/*.*",
        "*/amd64/*",
        "*/amd64/**/*.*",
        "linux/amd64/*.so",
        "darwin/x86_64/*.dylib",
        "META-INF/native/linux64/*",
        "META-INF/native/*x86_64*",
        "META-INF/native/*amd64*",
        "META-INF/elide/embedded/runtime/*/*-amd64*",
      )

      else -> arrayOf(
        "*/arm64/*",
        "*/aarch64/*",
        "linux/aarch64/*.so",
        "darwin/aarch64/*.dylib",
        "META-INF/native/*aarch_64*",
        "META-INF/native/*arm64*",
        "META-INF/elide/embedded/runtime/*/*-aarch64*",
      )
    }),
  )
}

fun CreateStartScripts.applyStartScriptSettings() {
  applicationName = "elide"
  optsEnvironmentVar = "ELIDE_OPTS"
  defaultJvmOpts = jvmDefs.map { "-D${it.key}=${it.value}" }
  unixStartScriptGenerator = UnixStartScriptGenerator().apply {
    template = resources.text.fromFile(layout.projectDirectory.file("packaging/unixStartScript.txt"))
  }
  windowsStartScriptGenerator = WindowsStartScriptGenerator().apply {
    template = resources.text.fromFile(layout.projectDirectory.file("packaging/windowsStartScript.bat"))
  }
}

fun Jar.applyJarSettings() {
  // include collected reachability metadata
  duplicatesStrategy = EXCLUDE

  if (enableResourceFilter) {
    filterResources(properties["elide.targetArch"] as? String)
  }

  manifest {
    attributes(
      "Elide-Engine-Version" to "v3",
      "Elide-Release-Track" to "ALPHA",
      "Elide-Release-Version" to version,
      "Application-Name" to "Elide",
      "Specification-Title" to "Elide VM Specification",
      "Specification-Version" to "0.1",
      "Implementation-Title" to "Elide VM Specification",
      "Implementation-Version" to "0.1",
      "Implementation-Vendor" to "Elide Technologies, Inc",
      "Application-Name" to "Elide",
      "Codebase" to "https://github.com/elide-dev/elide",
    )
  }
}

tasks {
  val ensureNatives by registering {
    doLast {
      val out = layout.buildDirectory.dir(nativesPath).get().asFile
      if (!out.exists()) {
        Files.createDirectories(out.toPath())
      }
    }
  }

  nativeCompile {
    dependsOn(ensureNatives)

    doFirst {
      val args = nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs)
      if (nativeImageBuildVerbose) {
        logger.lifecycle("Native Image args (dev/debug):\n${args.joinToString("\n")}")
      }
    }
  }

  nativeOptimizedCompile {
    dependsOn(ensureNatives)

    if (nativeImageBuildVerbose) doFirst {
      val args = nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs)
      logger.lifecycle("Native Image args (release):\n${args.joinToString("\n")}")
    }
  }

  processResources {
    filterResources()
  }

  jar {
    from(collectReachabilityMetadata)
  }

  listOf(
    startScripts,
    createOptimizedStartScripts,
  ).forEach {
    it.configure {
      applyStartScriptSettings()
    }
  }

  listOf(
    jar,
    optimizedJitJar,
    optimizedNativeJar,
  ).forEach {
    it.configure {
      applyJarSettings()
    }
  }

  compileJava {
    options.javaModuleVersion = provider { version as String }
    if (enableJpms) modularity.inferModulePath = true
  }

  named("run", JavaExec::class).configure {
    dependsOn(ensureNatives)

    jvmDefs.forEach {
      systemProperty(it.key, it.value)
    }

    systemProperty(
      "micronaut.environments",
      "dev",
    )
    systemProperty(
      "picocli.ansi",
      "tty",
    )
    systemProperty(
      "elide.nativeTransport.v2",
      enableNativeTransportV2.toString(),
    )
    jvmDefs.map {
      systemProperty(it.key, it.value)
    }
    systemProperty(
      "org.graalvm.language.ruby.home",
      layout.buildDirectory.dir("native/$nativeTargetType/resources/ruby/ruby-home").get().asFile.path.toString(),
    )
    systemProperty(
      "org.graalvm.language.python.home",
      layout.buildDirectory.dir("native/$nativeTargetType/resources/python/python-home").get().asFile.path.toString(),
    )
    systemProperty(
      "elide.natives",
      nativesPath,
    )
    systemProperty(
      "java.library.path",
      listOf(
        umbrellaNativesPath,
        nativesPath,
      ).plus(
        languagePluginPaths.map {
          it.toString()
        }
      ).plus(
        System.getProperty("java.library.path", "").split(File.pathSeparator).filter {
          it.isNotEmpty()
        }
      ).joinToString(
        File.pathSeparator
      )
    )

    jvmArgs(jvmModuleArgs)

    standardInput = System.`in`
    standardOutput = System.out

    classpath(
      configurations.compileClasspath,
      configurations.runtimeClasspath,
      jvmOnly,
    )
  }

  optimizedRun {
    dependsOn(ensureNatives)

    systemProperty(
      "micronaut.environments",
      "dev",
    )
    systemProperty(
      "org.graalvm.language.ruby.home",
      layout.buildDirectory.dir("native/$nativeTargetType/resources/ruby/ruby-home").get().asFile.path.toString(),
    )
    systemProperty(
      "org.graalvm.language.python.home",
      layout.buildDirectory.dir("native/$nativeTargetType/resources/python/python-home").get().asFile.path.toString(),
    )
    systemProperty(
      "elide.natives",
      nativesPath,
    )
    systemProperty(
      "java.library.path",
      listOf(
        umbrellaNativesPath,
        nativesPath,
      ).plus(
        System.getProperty("java.library.path", "").split(File.pathSeparator).filter {
          it.isNotEmpty()
        }
      ).joinToString(File.pathSeparator)
    )

    jvmDefs.map {
      systemProperty(it.key, it.value)
    }

    jvmArgs(jvmModuleArgs)

    standardInput = System.`in`
    standardOutput = System.out

    classpath(
      configurations.compileClasspath,
      configurations.runtimeClasspath,
      jvmOnly,
    )
  }

  withType(org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class).configureEach {
    compilerOptions {
      allWarningsAsErrors = true
      freeCompilerArgs.set(freeCompilerArgs.get().plus(ktCompilerArgs).toSortedSet().toList())
    }
  }

  nativeOptimizedCompile {
    dependsOn(decompressProfiles)
  }

  withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(jvmCompileArgs.plus(jvmOnlyCompileArgs))
  }

  withType<KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.set(freeCompilerArgs.get().plus(ktCompilerArgs).toSortedSet().toList())
    }
  }

  // Only built dists if specifically requested,
  listOf(
    distZip,
    distTar,
    optimizedDistZip,
    optimizedDistTar,
  ).forEach {
    it.configure {
      isEnabled = gradle.startParameter.taskNames.any { it.contains(name) }
    }
  }

  // Only run native builds if specifically requested on the command line.
  listOf(
    nativeCompile,
    nativeOptimizedCompile,
  ).forEach {
    it.configure {
      isEnabled = gradle.startParameter.taskNames.any { it.contains(name) }
    }
  }
}

listOf(
  configurations.compileClasspath,
  configurations.runtimeClasspath,
  configurations.nativeImageClasspath,
).forEach {
  it.get().apply {
    exclusions.forEach { exclusion ->
      exclusion.get().let { dep ->
        exclude(group = dep.group, module = dep.name)
      }
    }
  }
}
