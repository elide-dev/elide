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
  "UNUSED_PARAMETER",
)

import io.micronaut.gradle.MicronautRuntime
import org.apache.tools.ant.taskdefs.condition.Os
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Files
import java.nio.file.Path
import elide.internal.conventions.kotlin.KotlinTarget
import elide.toolchain.host.Criteria
import elide.toolchain.host.TargetCriteria
import elide.toolchain.host.TargetInfo

plugins {
  java
  `java-library`
  publishing
  jacoco
  `jvm-test-suite`
  `maven-publish`

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.micronaut.minimal.application)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.elide.conventions)
}

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
// - `elide.optMode`: Custom optimization mode
// - `elide.pgo`: Turn on/off Profile Guided Optimization
//
// Environment respected by this script:
//
// - `CC`: C compiler
// - `LD`: Linker
// - `CFLAGS`: C compiler flags

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
val archTripleToken = (findProperty("elide.arch") as? String)
  ?: if (System.getProperty("os.arch") == "aarch64") "aarch64" else "x86_64"
val muslTarget = "$archTripleToken-unknown-linux-musl"
val gnuTarget = "$archTripleToken-unknown-linux-gnu"
val macTarget = "$archTripleToken-apple-darwin"
val winTarget = "$archTripleToken-pc-windows-gnu"
val nativeTargetType = if (isRelease) "nativeOptimizedCompile" else "nativeCompile"
val entrypoint = "elide.tool.cli.MainKt"

val enablePkl = true
val enableJs = true
val enableJsIsolate = false
val enableTs = true
val enablePython = false
val enablePythonDynamic = true
val enableRuby = false
val enableLlvm = false
val enableJvm = false
val enableKotlin = false
val enableSqlite = true
val enableCustomCompiler = findProperty("elide.compiler") != null
val enableNativeCryptoV2 = false
val enableNativeTransportV2 = false
val enableSqliteStatic = true
val enableStatic = false
val enableStaticJni = true
val preferShared = true
val enableToolchains = false
val forceFfm = false
val enableClang = false
val oracleGvm = true
val oracleGvmLibs = oracleGvm
val enableMosaic = false
val enableEdge = false
val enableStage = true
val enableDynamicPlugins = false
val enableDeprecated = false
val enableJit = true
val enablePreinitializeAll = true
val enableExperimental = false
val enableExperimentalLlvmBackend = false
val enableExperimentalLlvmEdge = false
val enableFfm = hostIsLinux && System.getProperty("os.arch") != "aarch64" && enableExperimental
val enableEmbeddedResources = false
val enableResourceFilter = false
val enableAuxCache = true
val enableAuxCacheTool = false
val enableJpms = false
val enableConscrypt = false
val enableBouncycastle = false
val enableEmbeddedBuilder = false
val enableBuildReport = true
val enableHeapReport = false
val enableG1 = oracleGvm && !enableAuxCache
val enablePreinit = true
val enablePgo = findProperty("elide.pgo") != "false"
val enablePgoSampling = false
val enablePgoInstrumentation = false
val enableJna = true
val enableJnaJpms = false
val enableJnaStatic = false
val enableSbom = oracleGvm
val enableSbomStrict = false
val jniDebug = false
val glibcTarget = "glibc"
val dumpPointsTo = false
val elideTarget = TargetInfo.current(project)
val defaultArchTarget = when {
  TargetCriteria.allOf(elideTarget, Criteria.Amd64) -> "x86-64-v3"
  TargetCriteria.allOf(elideTarget, Criteria.MacArm64) -> "armv8.1-a"
  else -> "compatibility"
}

val optMode = findProperty("elide.optMode") as? String ?: "3"
val nativeOptMode = optMode

val elideBinaryArch = project.properties["elide.march"] as? String ?: defaultArchTarget

val exclusions = listOfNotNull(
  // always exclude the jline native lib; we provide it ourselves
  libs.jline.native,
  libs.jline.terminal.jna,

  // prefer jpms jna when enabled
  if (enableJna) null else libs.jna.jpms,

  // only include jline jni integration if ffm is disabled
  if (enableFfm && forceFfm) libs.jline.terminal.jni else null,

  // exclude kotlin compiler if kotlin is not enabled; it includes shadowed jline configs
  if (enableKotlin) null else libs.kotlin.compiler.embedded,
)

// Java Launcher (GraalVM at either EA or LTS)
val edgeJvmTarget = 25
val stableJvmTarget = 23
val edgeJvm = JavaVersion.toVersion(edgeJvmTarget)
val stableJvm = JavaVersion.toVersion(stableJvmTarget)
val selectedJvmTarget = if (enableEdge) edgeJvmTarget else stableJvmTarget
val selectedJvm = if (enableEdge) edgeJvm else stableJvm

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

val rootPath: String = rootProject.layout.projectDirectory.asFile.path
val sqliteLibPath: String = rootProject.layout.projectDirectory.dir("third_party/sqlite/install/lib").asFile.path

val jvmOnlyCompileArgs: List<String> = listOfNotNull(
  // Nothing at this time.
)

val jvmCompileArgs = listOfNotNull(
  "--enable-preview",
  "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  // "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  // "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  // "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
).plus(if (enableJpms) listOf(
  "--add-reads=elide.cli=ALL-UNNAMED",
  "--add-reads=elide.graalvm=ALL-UNNAMED",
) else emptyList()).plus(if (enableEmbeddedBuilder) listOf(
  "--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
) else emptyList())

val jvmRuntimeArgs = listOf(
  "-XX:ParallelGCThreads=2",
  "-XX:ConcGCThreads=2",
  "-XX:ReservedCodeCacheSize=512m",
)

val nativeCompileJvmArgs = listOf(
  "--enable-native-access=" + listOfNotNull(
    "org.graalvm.truffle,ALL-UNNAMED",
  ).joinToString(","),
).plus(jvmCompileArgs.map {
  "-J$it"
})

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
)

elide {
  kotlin {
    powerAssert = true
    atomicFu = true
    target = KotlinTarget.JVM
    customKotlinCompilerArgs = ktCompilerArgs
  }

  jvm {
    target = JVM_23
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

val targetPath: String = when {
  HostManager.hostIsLinux -> when (enableStatic) {
    // if we are targeting a fully static environment (i.e. musl), we need to sub in the target name in the path as we
    // are always technically cross-compiling.
    true -> rootProject.layout.projectDirectory.dir("target/$muslTarget/$nativesType")

    // otherwise, we use gnu's target.
    false -> rootProject.layout.projectDirectory.dir("target/$gnuTarget/$nativesType")
  }.asFile.path

  HostManager.hostIsMac -> rootProject.layout.projectDirectory.dir("target/$macTarget/$nativesType").asFile.path
  HostManager.hostIsMingw -> rootProject.layout.projectDirectory.dir("target/$winTarget/$nativesType").asFile.path
  else -> error("Unsupported platform for target path")
}

val nativesPath = targetPath

val umbrellaNativesPath: String = targetPath
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
  buildConfigField("String", "GVM_RESOURCES", "\"${gvmResourcesPath}\"")
}

val pklDependencies: Configuration by configurations.creating

val classpathExtras: Configuration by configurations.creating {
  extendsFrom(configurations.runtimeClasspath.get())
}

val jvmOnly: Configuration by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

dependencies {
  aotApplication(libs.graalvm.svm)
  aotApplication(libs.graalvm.truffle.runtime.svm)
  aotApplication(libs.graalvm.compiler)

  kapt(mn.micronaut.inject.java)
  kapt(libs.picocli.codegen)
  classpathExtras(mn.micronaut.core.processor)

  api(libs.clikt)
  api(libs.picocli)
  api(libs.guava)
  api(projects.packages.base)
  api(mn.micronaut.inject)
  implementation(projects.packages.terminal)
  jvmOnly(libs.snakeyaml)

  // Native-image transitive compile dependencies
  nativeImageCompileOnly(libs.jakarta.validation)
  nativeImageCompileOnly(libs.guava)

  implementation(libs.picocli.jansi.graalvm) {
    exclude(group = "org.fusesource.jansi", module = "jansi")
  }

  fun ExternalModuleDependency.pklExclusions() {
    exclude("org.pkl-lang", "pkl-server")
  }

  if (enablePkl) {
    listOf(
      libs.pkl.core,
      libs.pkl.commons.cli,
      libs.pkl.cli,
    ).forEach {
      implementation(it) { pklExclusions() }
    }
    pklDependencies(libs.pkl.cli) { pklExclusions() }
  }

  implementation(mn.micronaut.picocli)
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.logback)
  runtimeOnly(mn.micronaut.runtime)

  implementation(libs.jline.reader)
  implementation(libs.jline.console)
  implementation(libs.jline.terminal.core)
  implementation(libs.jline.terminal.jni)
  if (enableFfm) {
    implementation(libs.jline.terminal.ffm)
  }
  implementation(libs.jline.builtins)
  implementation(libs.jline.terminal.jansi) {
    exclude(group = "org.fusesource.jansi", module = "jansi")
  }

  // SQLite Engine
  if (enableSqlite) {
    implementation(projects.packages.sqlite)
  }
  if (enableNativeCryptoV2) {
    api(project(":packages:tcnative"))
  } else {
    implementation(libs.netty.tcnative)
  }

  // GraalVM: Engines
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmTs)
  implementation(projects.packages.graalvmWasm)
  api(libs.graalvm.polyglot)
  api(libs.graalvm.js.language)
  compileOnly(libs.graalvm.svm)

  if (oracleGvm && oracleGvmLibs) {
    nativeImageClasspath(libs.graalvm.truffle.enterprise)
    if (enableJsIsolate) {
      implementation(libs.graalvm.js.isolate)
    }
  }

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

  // General
  implementation(libs.smartexception)
  implementation(libs.magicProgress)

  runtimeOnly(mn.micronaut.graal)
  implementation(mn.netty.handler)

  // JVM-only dependencies which are filtered for native builds.
  if (!enableJna) {
    jvmOnly(libs.jna.jpms)
  } else {
    // we have to include _some_ support for JNA in order to support oshi et al.
    implementation(libs.jna.jpms)
    if (enableJnaStatic) {
      implementation(libs.jna.graalvm)
    }
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
  testApi(project(":packages:graalvm", configuration = "testBase"))
  testApi(project(":packages:engine", configuration = "testInternals"))

  if (!enableNativeCryptoV2) {
    implementation(libs.netty.tcnative.boringssl.static)
    implementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-x86_64") })
    implementation(variantOf(libs.netty.resolver.dns.native.macos) { classifier("osx-aarch_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-x86_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-aarch_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })
  }

  if (enableNativeTransportV2) {
    implementation(project(":packages:transport:transport-epoll"))
    implementation(project(":packages:transport:transport-kqueue"))
    implementation(project(":packages:transport:transport-uring"))
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

  if (oracleGvm && oracleGvmLibs && !enableEdge) {
    if (enableExperimental && hostIsLinux) {
      api(libs.graalvm.truffle.nfi.panama)
    }

    // Not provided at edge.
    // implementation(libs.graalvm.truffle.nfi.native.linux.aarch64)
    // implementation(libs.graalvm.truffle.nfi.native.darwin.amd64)
    // implementation(libs.graalvm.truffle.nfi.native.darwin.aarch64)
    // implementation(libs.graalvm.truffle.nfi.native.linux.amd64)
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
    possibleEnvironments = listOf("cli")

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

// Compiler environment
val cCompiler: String? = (findProperty("elide.compiler") as? String ?: System.getenv("CC")).also {
  if (enableCustomCompiler) require(it != null) { "Custom compiler is enabled, but no compiler is set." }
}
val cLinker: String? = (findProperty("elide.linker") as? String ?: System.getenv("LD"))
val cFlags: String? = System.getenv("CFLAGS")
val cxxCompiler: String? = System.getenv("CXX")
val cxxFlags: String? = System.getenv("CXXFLAGS")
val isClang: Boolean = cCompiler?.contains("clang") == true

val commonGvmArgs = listOfNotNull(
  "-H:+UseCompressedReferences",
  onlyIf(enableBuildReport, "--emit=build-report"),
).onlyIf(oracleGvm)

val nativeImageBuildDebug = properties["nativeImageBuildDebug"] == "true"
val nativeImageBuildVerbose = properties["nativeImageBuildVerbose"] == "true"

val stagedNativeArgs: List<String> = listOfNotNull(
  "-H:+RemoveUnusedSymbols",
  "-H:+ParseRuntimeOptions",
  "-H:+JNIEnhancedErrorCodes",
  onlyIf(oracleGvm && enableAuxCache, "-H:ReservedAuxiliaryImageBytes=${8 * 1024 * 1024}"),
  onlyIf(enableExperimentalLlvmBackend, "-H:CompilerBackend=llvm"),
  onlyIf(enableExperimental, "-H:+LayeredBaseImageAnalysis"),
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
  // "-H:+LSRAOptimization",
  // "-H:+LIRProfileMethods",
  // "-H:+LIRProfileMoves",
  // "-H:+UseExperimentalReachabilityAnalysis",  // not supported by truffle feature
  // "-H:+UseReachabilityMethodSummaries",  // not supported by truffle feature
  // "-H:+VMContinuations",  // not supported with runtime compilation
)

val deprecatedNativeArgs = listOf(
  "--configure-reflection-metadata",
  "--enable-all-security-services",
)

val enabledFeatures = listOfNotNull(
  "elide.tool.engine.MacLinkageFeature",
  "elide.tool.feature.ToolingUmbrellaFeature",
  "elide.runtime.feature.engine.NativeConsoleFeature",
  onlyIf(enablePython, "elide.runtime.feature.python.PythonFeature"),
  onlyIf(enableNativeTransportV2, "elide.runtime.feature.engine.NativeTransportFeature"),
  onlyIf(enableNativeCryptoV2, "elide.runtime.feature.engine.NativeCryptoFeature"),
  onlyIf(oracleGvm && oracleGvmLibs, "com.oracle.svm.enterprise.truffle.EnterpriseTruffleFeature"),
  onlyIf(oracleGvm && oracleGvmLibs, "com.oracle.truffle.runtime.enterprise.EnableEnterpriseFeature"),
  onlyIf(oracleGvm && enableExperimental, "com.oracle.svm.enterprise.truffle.PolyglotIsolateGuestFeature"),
  onlyIf(oracleGvm && enableExperimental, "com.oracle.svm.enterprise.truffle.PolyglotIsolateHostFeature"),
  onlyIf(enableJnaStatic && HostManager.hostIsMac, "com.sun.jna.SubstrateStaticJNA"),
  onlyIf(enableSqlite, "elide.runtime.feature.engine.NativeSQLiteFeature"),
)

val enabledSecurityProviders = listOfNotNull(
  onlyIf(enableBouncycastle, "org.bouncycastle.jce.provider.BouncyCastleProvider"),
  onlyIf(enableConscrypt, "org.conscrypt.OpenSSLProvider"),
)

val preinitializedContexts = if (!enablePreinit) emptyList() else listOfNotNull(
  "js",
  "ts",
  onlyIf(enablePreinitializeAll && enablePython, "python"),
  onlyIf(enablePreinitializeAll && enableRuby, "ruby"),
  onlyIf(enablePreinitializeAll && enableJvm, "java"),
)

val experimentalLlvmEdgeArgs = listOfNotNull(
  "-H:-CheckToolchain",
).plus(listOfNotNull(
  "-fuse-ld=lld",
  "-I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk",
  "-I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include",
  "-L/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/lib",
  "-L/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks",
  "-F/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks",
  "-Wno-nullability-completeness",
  "-Wno-deprecated-declarations",
  "-Wno-availability",
  "-Wno-unknown-warning-option",
  "-framework CoreFoundation",
  "-framework CFNetwork",
).map {
  "--native-compiler-options=$it"
})

val preinitContextsList = preinitializedContexts.joinToString(",")

val pluginApiHeader: String =
  rootProject.layout.projectDirectory.file("crates/substrate/headers/elide-plugin.h").asFile.path

val initializeAtBuildtime: List<String> = listOf(
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap\$1",
  "elide.tool.cli.AbstractToolCommand\$Companion",
  "elide.tool.cli.Elide\$Companion",
  "com.google.common.base.Equivalence\$Equals",
//  "org.xml.sax.helpers.LocatorImpl",
//  "org.xml.sax.helpers.AttributesImpl",
  "kotlin",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueEntry\$Helper",
  "ch.qos.logback",
//  "com.google.common",
//  "com.google.protobuf",
  "com.sun.tools.javac.resources.compiler",
  "com.sun.tools.javac.resources.javac",
  "sun.tools.jar.resources.jar",
  "sun.awt.resources.awt",
//  "elide",
//  "elide.tool",
  "elide.tool.cli.Elide",
  "elide.tool.cli.cmd.tool.EmbeddedTool\$Companion",
//  "elide.runtime",
//  "elide.runtime.gvm",
//  "elide.runtime.gvm.internals",
  "elide.runtime.gvm.internals.sqlite",
  "elide.runtime.gvm.internals.sqlite.SqliteModule",
  "elide.runtime.lang.javascript.JavaScriptPrecompiler",
//  "com.github.ajalt.mordant.internal.nativeimage.NativeImagePosixMppImpls",
//  "tools.elide",
//  "org.fusesource",
//  "org.jline",
  "java.sql",
  "org.slf4j",
  "org.sqlite",
  "org.pkl.core.runtime.VmLanguageProvider",
  "elide.runtime.lang.typescript",
  "elide.runtime.typescript",
)

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
)

val initializeAtRuntime: List<String> = listOfNotNull(
  onlyIf(!enableSqliteStatic, "org.sqlite.SQLiteJDBCLoader"),
  onlyIf(!enableSqliteStatic, "org.sqlite.core.NativeDB"),
  onlyIf(enableNativeTransportV2, "io.netty.channel.kqueue.Native"),
  onlyIf(enableNativeTransportV2, "io.netty.channel.kqueue.KQueueEventLoop"),
  "org.fusesource.jansi.internal.CLibrary",
  "com.github.ajalt.mordant.rendering.TextStyles",
  "elide.tool.err.ErrPrinter",
  "com.google.protobuf.RuntimeVersion",

  // @TODO: seal this into formal config
  "elide.tool.err.ErrorHandler${'$'}ErrorContext",

  // pkl needs this
  "org.msgpack.core.buffer.DirectBufferAccess",

  // --- JNA -----

  "com.sun.jna.Native",
  "com.sun.jna.Structure${'$'}FFIType",
  "com.sun.jna.platform.mac.IOKit",
  "com.sun.jna.platform.mac.IOKitUtil",
  "com.sun.jna.platform.mac.SystemB",
  "com.sun.jna.platform.linux.Udev",

  // --- Jansi/JLine -----

  "org.jline.nativ.JLineLibrary",
  "org.jline.nativ.CLibrary",
  "org.jline.nativ.CLibrary${'$'}WinSize",
  "org.jline.nativ.CLibrary${'$'}Termios",
  "org.jline.terminal.impl.jna.osx.OsXNativePty",
  "org.jline.terminal.impl.jna.linux.LinuxNativePty",
  "org.jline.terminal.impl.jna.linux.LinuxNativePty${'$'}UtilLibrary",
  "org.jline.terminal.impl.jna.solaris.SolarisNativePty",
  "org.jline.terminal.impl.jna.freebsd.FreeBsdNativePty",
  "org.jline.terminal.impl.jna.freebsd.FreeBsdNativePty${'$'}UtilLibrary",
  "org.jline.terminal.impl.jna.openbsd.OpenBsdNativePty",
  "org.jline.nativ.Kernel32",
  "org.fusesource.jansi.AnsiConsole",
  "org.fusesource.jansi.internal.Kernel32",
  "org.fusesource.jansi.io.WindowsAnsiProcessor",

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
  "oshi.jna.platform.linux.LinuxLibc",
  "oshi.util.UserGroupInfo",
  "oshi.driver.linux.Lshw",
  "com.sun.jna.platform.linux.LibC",
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
  "org.jetbrains.kotlin",

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

  // --- JLine/Jansi -----

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
  "org.fusesource.jansi.internal.CLibrary",
  "org.fusesource.jansi.internal.CLibrary${'$'}WinSize",
  "org.fusesource.jansi.internal.CLibrary${'$'}Termios",
  "org.fusesource.jansi.internal.Kernel32",
  "org.fusesource.jansi.internal.Kernel32${'$'}CHAR_INFO",
  "org.fusesource.jansi.internal.Kernel32${'$'}CONSOLE_SCREEN_BUFFER_INFO",
  "org.fusesource.jansi.internal.Kernel32${'$'}COORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}FOCUS_EVENT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}INPUT_EVENT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}INPUT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}KEY_EVENT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}MENU_EVENT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}MOUSE_EVENT_RECORD",
  "org.fusesource.jansi.internal.Kernel32${'$'}SMALL_RECT",
  "org.fusesource.jansi.internal.Kernel32${'$'}WINDOW_BUFFER_SIZE_RECORD",

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

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntimeTest: List<String> = emptyList()

val commonNativeArgs = listOfNotNull(
  // Debugging flags:
  "--verbose",
  "-H:TempDirectory=/tmp/elide-native",
  // "-H:+PlatformInterfaceCompatibilityMode",
  // "--trace-object-instantiation=sun.awt.resources.awt",
  // "--trace-object-instantiation=com.sun.tools.javac.resources.compiler",
  onlyIf(enableCustomCompiler && !cCompiler.isNullOrEmpty(), "--native-compiler-path=$cCompiler"),
  onlyIf(isDebug, "-H:+JNIVerboseLookupErrors"),
  onlyIf(!enableJit, "-J-Dtruffle.TruffleRuntime=com.oracle.truffle.api.impl.DefaultTruffleRuntime"),
  onlyIf(enableFfm, "-H:+ForeignAPISupport"),
  onlyIf(dumpPointsTo, "-H:PrintAnalysisCallTreeType=CSV"),
  onlyIf(dumpPointsTo, "-H:+PrintImageObjectTree"),
  onlyIf(enabledFeatures.isNotEmpty(), "--features=${enabledFeatures.joinToString(",")}"),
  // Common flags:
  "-march=$elideBinaryArch",
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  // @TODO breaks with old configs, and new configs can't use the agent.
  // "--exact-reachability-metadata",
  "--enable-url-protocols=http,https",
  "--color=always",
  "-H:+UnlockExperimentalVMOptions",
  "--link-at-build-time=elide",
  "--link-at-build-time=dev.elide",
  "--link-at-build-time=org.pkl",
  "--link-at-build-time=picocli",
  onlyIf(enableJnaJpms, "--enable-native-access=org.graalvm.truffle,ALL-UNNAMED") ?: "--enable-native-access=org.graalvm.truffle,ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  "-H:+PreserveFramePointer",
  "-H:+ReportExceptionStackTraces",
  "-H:+AddAllCharsets",
  "-H:-ReduceImplicitExceptionStackTraceInformation",
  "-H:MaxRuntimeCompileMethods=20000",
  "-H:ExcludeResources=META-INF/native/libumbrella.so",
  "-H:ExcludeResources=META-INF/native/libumbrella.a",
  "-H:ExcludeResources=kotlin/kotlin.kotlin_builtins",
  "-H:ExcludeResources=META-INF/.*kotlin_module",
  "-H:ExcludeResources=com/sun/jna/.*",
  "-H:ExcludeResources=META-INF/native/libsqlite.*",
  "-H:ExcludeResources=META-INF/micronaut/io.micronaut.core.graal.GraalReflectionConfigurer/.*",
  "-Delide.strict=true",
  "-Delide.js.vm.enableStreams=true",
  "-Delide.mosaic=$enableMosaic",
  "-Delide.staticJni=$enableStaticJni",
  "-Delide.preferShared=$preferShared",
  "-Delide.lang.javascript=$enableJs",
  "-Delide.lang.typescript=$enableTs",
  "-Delide.lang.python=$enablePython",
  "-Delide.lang.ruby=$enableRuby",
  "-Delide.lang.jvm=$enableJvm",
  "-Delide.lang.llvm=$enableLlvm",
  "-Delide.lang.kotlin=$enableKotlin",
  "-Delide.lang.pkl=$enablePkl",
  "-Delide.lang.experimental=$enableExperimental",
  "-Delide.staticUmbrella=false",
  "-Delide.root=$rootPath",
  "-Delide.target=$targetPath",
  "-Delide.natives=$nativesPath",
  "-Delide.natives.pluginApiHeader=$pluginApiHeader",
  "-Djna.library.path=$nativesPath",
  "-Djna.boot.library.path=$nativesPath",
  "-Dorg.sqlite.lib.path=$nativesPath",
  "-Dorg.sqlite.lib.exportPath=$nativesPath",
  "-Dio.netty.native.workdir=$nativesPath",
  "-Dlibrary.jansi.path=$nativesPath",
  "-Dlibrary.jline.path=$nativesPath",
  "-Dio.netty.native.deleteLibAfterLoading=false",
  "-Dio.netty.allocator.type=adaptive",
  "-R:MaxDirectMemorySize=32G",
  "-Delide.nativeTransport.v2=${enableNativeTransportV2}",
  "-Dtruffle.TrustAllTruffleRuntimeProviders=true",
  "-Dgraalvm.locatorDisabled=false",
  "-Dpolyglotimpl.DisableVersionChecks=false",
  "-Dnetty.default.allocator.max-order=3",
  "-Dnetty.resource-leak-detector-level=DISABLED",
  "-Dmicronaut.server.netty.use-native-transport=true",
  "-Dmicronaut.server.netty.parent.prefer-native-transport=true",
  "-Dmicronaut.server.netty.worker.prefer-native-transport=true",
  "-Dmicronaut.netty.event-loops.default.prefer-native-transport=true",
  "-Dmicronaut.netty.event-loops.default.num-threads=2",
  "-Dmicronaut.netty.event-loops.parent.num-threads=2",
  "-Djackson.serialization.ORDER_MAP_ENTRIES_BY_KEYS=true",
  "-Dmicronaut.application.name=elide",
  "-Dmicronaut.application.default-charset=utf-8",
  "-Dmicronaut.executors.default.threads=1",
  "-Dmicronaut.executors.default.type=VIRTUAL",
  "-Dmicronaut.executors.io.threads=2",
  "-Dmicronaut.executors.io.type=VIRTUAL",
  "-Dmicronaut.executors.scheduled.threads=1",
  onlyIf(enablePreinit, "-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true"),
  onlyIf(enablePreinit, "-Dpolyglot.image-build-time.PreinitializeContexts=$preinitContextsList"),
  onlyIf(enablePgoInstrumentation, "--pgo-instrument"),
  onlyIf(enablePgoSampling, "--pgo-sampling"),
  onlyIf(enableHeapReport, "-H:+BuildReportMappedCodeSizeBreakdown"),
  onlyIf(enableHeapReport, "-H:+TrackNodeSourcePosition"),
  onlyIf(enableHeapReport, "-R:-TrackNodeSourcePosition"),
).asSequence().plus(
  languagePluginPaths.plus(listOf(umbrellaNativesPath)).filter {
    Files.exists(Path.of(it))
  }.map {
    "-H:CLibraryPath=$it"
  }
).plus(
  commonGvmArgs.onlyIf(oracleGvm)
).plus(
  listOf("--debug-attach").onlyIf(nativeImageBuildDebug)
).plus(
  nativeCompileJvmArgs
).plus(
  listOf(
    "-H:AdditionalSecurityProviders=${enabledSecurityProviders.joinToString(",")}",
  ).onlyIf(enabledSecurityProviders.isNotEmpty())
).toList()

val debugFlags: List<String> = listOfNotNull(
  "--verbose",
  "-g",
  "-H:+SourceLevelDebug",
  // "-Dpolyglot.engine.TraceCache=true",
  // "-J-Dpolyglot.engine.TraceCache=true",
  // "-H:-DeleteLocalSymbols",
  // "-H:+PrintMethodHistogram",
  // "-H:+PrintPointsToStatistics",
  // "-H:+PrintRuntimeCompileMethods",
  // "-H:+ReportPerformedSubstitutions",
).onlyIf(isDebug)

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

// C compiler flags which are always included.
val commonCFlags: List<String> = listOf(
  "-DELIDE",
  "-fstack-clash-protection",
  "-fstack-protector-strong",
  "-fexceptions",
  "-ffunction-sections",
  "-fdata-sections",
//  "-fno-omit-frame-pointer",
//  "-v",
//  "-fno-strict-aliasing",
//  "-fno-strict-overflow",
//  "-fno-delete-null-pointer-checks",
//  "-DELIDE",
  // "-Wno-hardened",
).plus(
  listOf("-fuse-ld=$cLinker").onlyIf(cLinker != null)
).plus(
  System.getenv("CFLAGS")?.ifEmpty { null }?.split(" ") ?: emptyList()
)

// Linker flags which are always included.
val commonLinkerOptions: List<String> = listOf()

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
  "-O$nativeOptMode",
  "-fPIC",
  "-fPIE",
  "-flto",
).plus(
  listOf("-fuse-linker-plugin").onlyIf(!enableClang && !isClang)
).plus(
  // Add protection flags for release.
  when (targetArch) {
    "x86_64", "amd64", "x86-64" -> listOf(
      "-fcf-protection=full",
    )
    "arm", "aarch64" -> listOf(
      "-mbranch-protection=standard",
    )
    else -> emptyList()
  }
)

// PGO profiles to specify in release mode.
val profiles: List<String> = listOf(
  "js-brotli.iprof",
  "pkl-eval.iprof",
  "ts-hello.iprof",
  "ts-sqlite.iprof",
)

// GVM release flags
val gvmReleaseFlags: List<String> = listOfNotNull(
  onlyIf(!enablePgo, "-O$optMode"),
)

// Experimental C-compiler flags.
val experimentalCFlags: List<String> = listOf(
  // Nothing at this time.
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
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

val jvmDefs = mutableMapOf(
  "elide.strict" to "true",
  "elide.natives" to nativesPath,
  "elide.root" to rootPath,
  "elide.target" to elideTarget.triple,
  "elide.graalvm.ee" to oracleGvm.toString(),
  "elide.mosaic" to enableMosaic.toString(),
  "elide.staticJni" to enableJnaStatic.toString(),
  "elide.js.vm.enableStreams" to "true",
  "elide.kotlin.version" to libs.versions.kotlin.sdk.get(),
  "elide.kotlin.verbose" to "false",
  "elide.nativeTransport.v2" to enableNativeTransportV2.toString(),
  "jna.library.path" to nativesPath,
  "jna.boot.library.path" to nativesPath,
  "io.netty.allocator.type" to "adaptive",
  "io.netty.native.deleteLibAfterLoading" to "false",
  "io.netty.native.detectNativeLibraryDuplicates" to "false",
  "io.netty.native.tryPatchShadedId" to "false",
  "java.net.preferIPv4Stack" to "true",
  "logback.statusListenerClass" to "ch.qos.logback.core.status.NopStatusListener",
  "networkaddress.cache.ttl" to "10",
  "polyglotimpl.DisableVersionChecks" to "false",
  "user.country" to "US",
  "user.language" to "en",
  "org.sqlite.lib.path" to nativesPath,
  "org.sqlite.lib.exportPath" to nativesPath,
  "io.netty.native.workdir" to nativesPath,
  "io.netty.native.deleteLibAfterLoading" to false.toString(),
  "java.util.concurrent.ForkJoinPool.common.parallelism" to "4",
  "java.util.concurrent.ForkJoinPool.common.maximumSpares" to "128",
  // "java.util.concurrent.ForkJoinPool.common.threadFactory" to "",
  // "java.util.concurrent.ForkJoinPool.common.exceptionHandler" to "",
)

findProperty("elide.logLevel")?.let {
  jvmDefs["elide.logging.root.level"] = it as String
}

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val pklArgs: List<String> = listOf(
  "-H:IncludeResourceBundles=org.pkl.core.errorMessages",
).plus(listOf(
  "org.pkl.core",
  "org.pkl.core.runtime.VmLanguageProvider",
  "org.antlr.v4",
  "org.snakeyaml",
  "org.organicdesign",
  "org.msgpack",
  "org.w3c.dom",
).map {
  "--initialize-at-build-time=$it"
})

val defaultPlatformArgs: List<String> = listOf()

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "--gc=serial",
  "-R:MaximumHeapSizePercent=80",
).plus(if (oracleGvm) listOf(
  "-Delide.vm.engine.preinitialize=true",
) else listOf(
  "-Delide.vm.engine.preinitialize=false",
)).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())).plus(if (oracleGvm) listOf(
  // disabled on windows
  "-H:-AuxiliaryEngineCache",
) else emptyList())

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "--gc=serial",
  "-R:MaximumHeapSizePercent=80",
  "--initialize-at-build-time=sun.awt.resources.awtosx",
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
    "-g",  // always generate debug info on linux
    "-H:NativeLinkerOption=-flto",
    "-H:NativeLinkerOption=-Wl,--gc-sections",
    "-H:NativeLinkerOption=-Wl,--emit-relocs",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libdiag.so",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libsqlitejdbc.so",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libumbrella.so",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libjs.so",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libposix.so",
    "-H:NativeLinkerOption=/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/libterminal.so",
    // "/home/sam/workspace/elide/target/x86_64-unknown-linux-gnu/debug/build/terminal-543d7c2278c5a0d2/out/libterminalcore.a",
    "-H:ExcludeResources=.*dylib",
    "-H:ExcludeResources=.*jnilib",
    "-H:ExcludeResources=.*dll",
    "-H:ExcludeResources=META-INF/native/libsqlitejdbc-linux-amd64.so",
    "-H:ExcludeResources=lib/osx-aarch64/libbrotli.dylib",
    "-H:ExcludeResources=META-INF/native/libnetty_transport_native_epoll_x86_64.so",
    "-H:ExcludeResources=META-INF/native/libnetty_transport_native_kqueue_x86_64.jnilib",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
  ).plus(
    listOfNotNull(
      onlyIf(enableStatic, "--libc=musl"),
      onlyIf(enableStatic, "--static"),
      onlyIf(!enableStatic, "--static-nolibc"),
    )
  ).plus(
    listOf(
      "-H:NativeLinkerOption=-lm",
      "-H:NativeLinkerOption=-lssl",
      "-H:NativeLinkerOption=-lcrypto",
      "-H:NativeLinkerOption=-lstdc++",
    ).onlyIf(!enableStatic)
  ),
).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:-AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=false",
  ) else listOf(
    "--gc=serial",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  ).plus(if (oracleGvm && enableAuxCache && !enableG1) listOf(
    "-H:+AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=true",
  ) else emptyList())
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx36g",
  "--parallelism=30",
))

val linuxGvmReleaseFlags = listOf(
  "-H:+ObjectInlining",
)

val linuxReleaseArgs = linuxOnlyArgs.plus(
  listOf(
    "-R:+WriteableCodeCache",
  ).plus(if (oracleGvm) linuxGvmReleaseFlags else emptyList()),
)

val muslArgs = listOf(
  "--static",
  "--libc=musl",
)

val testOnlyArgs: List<String> = emptyList()

val nativeOverrideArgs: List<String> = listOf()

fun nativeCliImageArgs(
  platform: String = "generic",
  target: String = glibcTarget,
  debug: Boolean = isDebug,
  test: Boolean = false,
  sharedLib: Boolean = false,
  release: Boolean = isRelease,
): List<String> =
  commonNativeArgs.asSequence()
    .plus(deprecatedNativeArgs.onlyIf(enableDeprecated))
    .plus(stagedNativeArgs.onlyIf(enableStage))
    .plus(jvmCompileArgs)
    .plus(pklArgs.onlyIf(enablePkl))
    .plus(listOf(
      targetPath,
      sqliteLibPath,
    ).plus(
      languagePluginPaths
    ).plus(
      System.getProperty("java.library.path", "")
        .split(File.pathSeparator)
        .filter { it.isNotEmpty() }
    ).distinct().joinToString(
      File.pathSeparator
    ).let {
      listOf(
        "-Djava.library.path=$it",
      )
    }
  ).plus(
    jvmCompileArgs.map { "-J$it" },
  ).plus(
    initializeAtBuildtime.map { "--initialize-at-build-time=$it" }
  ).plus(
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
    commonCFlags.map { "--native-compiler-options=$it" }
  ).plus(
    commonLinkerOptions.map { "-H:NativeLinkerOption=$it" }
  ).plus(
    when {
      release -> releaseFlags
      debug -> debugFlags
      else -> emptyList()
    }
  ).plus(
    nativeOverrideArgs
  ).plus(
    experimentalLlvmEdgeArgs.onlyIf(enableExperimentalLlvmEdge)
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

    val userCodeFilter = layout.projectDirectory.file("src/config/user-code-filter.json").asFile.path

    enabled = (
      (System.getenv("GRAALVM_AGENT") == "true" || project.properties.containsKey("agent")) &&
      !gradle.startParameter.isProfile  // profiler for build can't also be active
    )

    modes {
      standard {}

      conditional {
        userCodeFilterPath = userCodeFilter
      }
    }
    metadataCopy {
      inputTaskNames.addAll(listOf("run", "optimizedRun"))
      outputDirectories.add("src/main/resources/META-INF/native-image/dev/elide/elide-cli")
      mergeWithExisting = false
    }
  }

  binaries {
    named("main") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      if (enableToolchains) javaLauncher = gvmLauncher

      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      )

      // compute main compile args
      buildArgs.addAll(nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs))
    }

    create("shared") {
      imageName = "libelidemain"
      sharedLibrary = true
      quickBuild = quickbuild
      fallback = false
      if (enableToolchains) javaLauncher = gvmLauncher

      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      )

      buildArgs.addAll(nativeCliImageArgs(
        debug = quickbuild,
        sharedLib = true,
        release = !quickbuild,
        platform = targetOs))
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      if (enableToolchains) javaLauncher = gvmLauncher
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true, platform = targetOs))
      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      )
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

tasks.nativeCompile.configure {
  useArgFile = true
}

val decompressProfiles: TaskProvider<Copy> by tasks.registering(Copy::class) {
  from(zipTree(layout.projectDirectory.file("profiles.zip")))
  into(layout.buildDirectory.dir("native/nativeOptimizedCompile"))
}

val excludedStatics = arrayOf(
  "about.html",
  "plugin.xml",
)

fun hostTargetResources(): Array<String> = TargetInfo.current(project).resources.toTypedArray()

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
  nativeCompile {
    doFirst {
      val args = nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs)
      if (nativeImageBuildVerbose) {
        logger.lifecycle("Native Image args (dev/debug):\n${args.joinToString("\n")}")
      }
    }
  }

  nativeOptimizedCompile {
    if (nativeImageBuildVerbose) doFirst {
      val args = nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs)
      logger.lifecycle("Native Image args (release):\n${args.joinToString("\n")}")
    }
  }

  processResources {
    dependsOn(
      ":packages:graalvm:buildRustNativesForHost",
    )
    filterResources()

    // add `[lib]umbrella.{so,dylib,jnilib,dll}` to the jar
    // from(targetPath) {
    //   include("libumbrella.so")
    //   include("libumbrella.dylib")
    //   include("libumbrella.jnilib")
    //   include("umbrella.dll")
    //   into("META-INF/native/")
    // }
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
    if (enableToolchains) javaLauncher = gvmLauncher

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
      "java.library.path",
      listOf(
        umbrellaNativesPath,
        sqliteLibPath,
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

    jvmArgs(jvmModuleArgs.plus(
      listOf(
        "-verbose:jni",
        "-Xlog:library=trace",
      ).onlyIf(jniDebug)
    ))

    standardInput = System.`in`
    standardOutput = System.out

    classpath(
      configurations.compileClasspath,
      configurations.runtimeClasspath,
      jvmOnly,
    )
  }

  test {
    if (enableToolchains) javaLauncher = gvmLauncher

    jvmDefs.forEach {
      systemProperty(it.key, it.value)
    }

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
      "java.library.path",
      listOf(
        umbrellaNativesPath,
        sqliteLibPath,
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
    if (jniDebug) {
      jvmArgs(
        "-verbose:jni",
        "-Xlog:library=trace",
      )
    }
  }

  optimizedRun {
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
      "java.library.path",
      listOf(
        umbrellaNativesPath,
        sqliteLibPath,
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

listOf(
  tasks.withType<BuildNativeImageTask>()
).forEach {
  it.configureEach {
    dependsOn(
      ":packages:graalvm:natives",
    )
  }
}
