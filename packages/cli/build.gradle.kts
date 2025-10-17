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
  "unused",
  "UnstableApiUsage",
  "UNUSED_PARAMETER",
  "SpreadOperator",
  "MagicNumber",
)

import io.micronaut.gradle.MicronautRuntime
import org.apache.tools.ant.taskdefs.condition.Os
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.graalvm.buildtools.gradle.tasks.GenerateResourcesConfigFile
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import elide.internal.conventions.kotlin.KotlinTarget
import elide.toolchain.host.Criteria
import elide.toolchain.host.TargetCriteria
import elide.toolchain.host.TargetInfo

plugins {
  java
  publishing
  `java-library`
  `jvm-test-suite`
  `maven-publish`

  kotlin("jvm")
  kotlin("plugin.serialization")
  alias(libs.plugins.asciidoctor)
  alias(libs.plugins.ksp)
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
// - `elide.targetLibc`: `glibc`, `musl`, `bionic`
// - `elide.static`: `true` or `false`
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

val isRelease = (
  project.properties["elide.release"] == "true" ||
  project.properties["elide.buildMode"] == "release"
)

val isDebug = !isRelease && (
  project.properties["elide.buildMode"] == "debug"
)

val hostIsLinux = HostManager.hostIsLinux
val hostIsMac = HostManager.hostIsMac
val hostIsWindows = HostManager.hostIsMingw
val nativesType = "debug"
//val nativesType = if (!isRelease) "debug" else "release"
val archTripleToken = (findProperty("elide.arch") as? String)
  ?: if (System.getProperty("os.arch") == "aarch64") "aarch64" else "x86_64"
val muslTarget = "$archTripleToken-unknown-linux-musl"
val gnuTarget = "$archTripleToken-unknown-linux-gnu"
val macTarget = "$archTripleToken-apple-darwin"
val winTarget = "$archTripleToken-pc-windows-gnu"
val nativeTargetType = if (isRelease) "nativeOptimizedCompile" else "nativeCompile"
val entrypoint = "elide.tool.cli.MainKt"
val commandMain = "elide.tool.cli.Elide"

val enablePkl = true
val enableJs = true
val enableJsIsolate = false
val enableTs = true
val enablePython = true
val enablePythonDynamic = true
val enableRuby = false
val enableLlvm = true
val enableJvm = true
val enableKotlin = true
val enableSqlite = true
val enableAllLocales = false
val enableLocaleSupport = false
val enableCustomCompiler = findProperty("elide.compiler") != null
val enableNativeCryptoV2 = false
val enableNativeTransportV2 = true
val enableSqliteStatic = true
val enableStatic = findProperty("elide.static") == "true"
val enableStaticJni = true
val preferShared = false
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
val enableEmbeddedSvm = true
val enableFfm = hostIsLinux && System.getProperty("os.arch") != "aarch64"
val enableEmbeddedResources = false
val enableResourceFilter = false
val enableAuxCache = false
val enableAuxCacheTrace = true
val enableJpms = false
val enableConscrypt = false
val enableBouncycastle = false
val enableEmbeddedJvm = false
val enableCliDocs = true
val enableBuildReport = true
val enableHeapReport = false
val enableG1 = false
val enablePreinit = true
val enablePgo = findProperty("elide.pgo") != "false"
val enablePgoSampling = false
val enablePgoInstrumentation = false
val enableJna = true
val enableJnaJpms = false
val enableJnaStatic = false
val enableSbom = oracleGvm
val enableSbomStrict = false
val enableJfr = true
val enableNmt = false
val enableHeapDump = false
val enableJvmstat = false
val enableJmx = false
val enableVerboseClassLoading = false
val jniDebug = false
val libcTarget = (findProperty("elide.targetLibc") as? String) ?: (if (enableStatic) "musl" else "glibc")
val dumpPointsTo = false
val elideTarget = TargetInfo.current(project)
val effectiveGc = findProperty("elide.gc") ?: "serial"
val defaultArchTarget = when {
  TargetCriteria.allOf(elideTarget, Criteria.Amd64) -> "x86-64-v3"
  TargetCriteria.allOf(elideTarget, Criteria.MacArm64) -> "armv8.1-a"
  else -> "compatibility"
}
val isArm = TargetCriteria.allOf(elideTarget, Criteria.Arm64)

val optMode = findProperty("elide.optMode") as? String ?: "3"
val nativeOptMode = when (val explicit = optMode) {
  "b" -> "0"
  else -> explicit
}

val elideBinaryArch = project.properties["elide.march"] as? String ?: defaultArchTarget
logger.lifecycle("Building for architecture '$elideBinaryArch' (default: '$defaultArchTarget')")

val exclusions = listOfNotNull(
  // always exclude the jline native lib; we provide it ourselves
  libs.jline.native,
)

// Java Launcher (GraalVM at either EA or LTS)
val edgeJvmTarget = 25
val stableJvmTarget = 25

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

// Modules for SVM exports.
val svmModules = "com.oracle.graal.graal_enterprise,com.oracle.svm.svm_enterprise"

// JPMS add-on arguments for SVM builder stuff
val jpmsSvmArgs = listOf(
  "jdk.compiler" to "com.sun.tools.javac.util",
  "jdk.compiler" to "com.sun.tools.javac.parser",
  "jdk.graal.compiler" to "jdk.graal.compiler.options",
  "java.base" to "jdk.internal.module",
  "jdk.internal.vm.ci" to "jdk.vm.ci.meta",
  "jdk.internal.vm.ci" to "jdk.vm.ci.code",
  "jdk.graal.compiler" to "jdk.graal.compiler.util",
  "jdk.graal.compiler" to "jdk.graal.compiler.serviceprovider",
  "jdk.graal.compiler" to "jdk.graal.compiler.util.json",
  "java.base" to "jdk.internal.jimage",
  "org.graalvm.nativeimage.builder" to "com.oracle.svm.hosted",
  "org.graalvm.nativeimage.builder" to "com.oracle.svm.core",
  "org.graalvm.nativeimage.builder" to "com.oracle.svm.core.util",
  "jdk.jfr" to "jdk.jfr.internal",
).map {
  "--add-exports=${it.first}/${it.second}=ALL-UNNAMED"
}.plus(
  // `--add-opens` amendments, where reflection is required for SVM's build driver
  listOf(
    "org.graalvm.nativeimage.builder" to "com.oracle.svm.hosted",
  ).map {
    "--add-opens=${it.first}/${it.second}=ALL-UNNAMED"
  }
).plus(
  listOf(
    ("java.base" to "jdk.internal.module") to "org.graalvm.nativeimage.base",
    ("jdk.internal.vm.ci" to "jdk.vm.ci.meta") to "org.graalvm.nativeimage.pointsto",
    ("jdk.internal.vm.ci" to "jdk.vm.ci.code") to "org.graalvm.nativeimage.pointsto",
    ("java.base" to "jdk.internal.jimage") to "org.graalvm.nativeimage.driver",
    ("jdk.graal.compiler" to "jdk.graal.compiler.util.json") to "com.oracle.graal.reporter",
    ("java.base" to "jdk.internal.misc") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.aarch64") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.amd64") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.code.site") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.code") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.common") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.hotspot") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.meta") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.riscv64") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.runtime") to svmModules,
    ("jdk.internal.vm.ci" to "jdk.vm.ci.services") to svmModules,
    ("jdk.jfr" to "jdk.jfr.internal") to "org.graalvm.nativeimage.builder,org.graalvm.nativeimage.driver",
    // ("" to "") to "",
  ).map {
    "--add-exports=${it.first.first}/${it.first.second}=${it.second}"
  }
)

val jvmCompileArgs = listOfNotNull(
  "--add-modules=jdk.unsupported",
  "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
  "--add-exports=java.base/jdk.internal.jrtfs=ALL-UNNAMED",
  "--add-exports=jdk.jartool/sun.tools.jar=ALL-UNNAMED",
  "--add-exports=jdk.javadoc/jdk.javadoc.internal.tool=ALL-UNNAMED",
).plus(if (enableJpms) listOf(
  "--add-reads=elide.cli=ALL-UNNAMED",
  "--add-reads=elide.graalvm=ALL-UNNAMED",
) else emptyList()).plus(
  jpmsSvmArgs.onlyIf(
    enableEmbeddedSvm
  )
)

val jvmRuntimeArgs = listOf(
  "-XX:+UnlockExperimentalVMOptions",
  "-XX:ParallelGCThreads=2",
  "-XX:ConcGCThreads=2",
  "-XX:ReservedCodeCacheSize=512m",
  "-XX:+TrustFinalNonStaticFields",
).plus(
  jpmsSvmArgs.onlyIf(
    enableEmbeddedSvm
  )
)

val nativeEnabledModules = listOf(
  "org.graalvm.truffle",
  "io.netty.common",
  "ALL-UNNAMED",
)

val nativeCompileJvmArgs = listOf(
  "--enable-native-access=" + nativeEnabledModules.joinToString(","),
).plus(
  jvmCompileArgs
).plus(jvmCompileArgs.map {
  "-J$it"
})

val jvmModuleArgs = jvmCompileArgs.plus(jvmRuntimeArgs)

val ktCompilerArgs = mutableListOf(
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",

  // opt-in to Elide's delicate runtime API
  "-opt-in=elide.runtime.core.DelicateElideApi",
)

elide {
  kotlin {
    powerAssert = true
    atomicFu = true
    target = KotlinTarget.JVM
    ksp = true
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

kotlin {
  compilerOptions {
    allWarningsAsErrors = true
    freeCompilerArgs.set(freeCompilerArgs.get().plus(ktCompilerArgs).toSortedSet().toList())
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
}

val pklDependencies: Configuration by configurations.creating
val svmModulePath: Configuration by configurations.creating
val cliCodeGenerator: Configuration by configurations.creating
val cliJitOptimized: Configuration by configurations.creating { isCanBeConsumed = true }
val cliNativeOptimized: Configuration by configurations.creating { isCanBeConsumed = true }
val embeddedKotlin: Configuration by configurations.creating { isCanBeResolved = true }
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

  annotationProcessor(libs.picocli.codegen)
  cliCodeGenerator(libs.picocli.codegen)

  ksp(mn.micronaut.inject.kotlin)
  classpathExtras(mn.micronaut.core.processor)

  api(mn.micronaut.http.netty)
  api(libs.clikt)
  api(libs.picocli)
  api(libs.guava)
  api(projects.packages.base)
  api(mn.micronaut.inject)
  implementation(projects.packages.terminal)
  implementation(projects.packages.localAi)
  implementation(projects.packages.telemetry)
  implementation(projects.packages.runner)
  implementation(libs.dirs)
  implementation(libs.snakeyaml.core)
  implementation(mn.micronaut.json.core)
  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.bundles.mordant)
  implementation(libs.classgraph)

  val gvmJarsRoot = rootProject.layout.projectDirectory.dir("third_party/oracle")
  val googJarsRoot = rootProject.layout.projectDirectory.dir("third_party/google")

  val patchedLibs = files(
    gvmJarsRoot.file("library-support.jar"),
    gvmJarsRoot.file("svm-driver.jar"),
    gvmJarsRoot.file("objectfile.jar"),
    gvmJarsRoot.file("pointsto.jar"),
    gvmJarsRoot.file("native-image-base.jar"),
    gvmJarsRoot.file("truffle-coverage.jar"),
    googJarsRoot.file("jib-plugins-common.jar"),
    googJarsRoot.file("jib-cli.jar"),
  )

  svmModulePath(patchedLibs)
  svmModulePath(libs.graalvm.svm)
  svmModulePath(libs.graalvm.shadowed.json)
  svmModulePath(mn.netty.common)
  jvmOnly(patchedLibs)
  embeddedKotlin(project(":packages:graalvm-kt", configuration = "embeddedKotlin"))

  // Native-image transitive compile dependencies
  implementation(libs.guava)

  implementation(libs.picocli.jansi.graalvm) {
    exclude(group = "org.fusesource.jansi", module = "jansi")
  }

  fun ExternalModuleDependency.pklExclusions() {
    exclude("org.pkl-lang", "pkl-server")
    exclude("org.pkl-lang", "pkl-config-java-all")
  }

  if (enablePkl) {
    listOf(
      libs.pkl.core,
      libs.pkl.commons.cli,
      libs.pkl.cli,
      libs.pkl.config.java,
      libs.pkl.config.kotlin,
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
  implementation(libs.jline.terminal.jna)

  if (enableFfm) {
    //implementation(libs.jline.terminal.ffm)
  }

  implementation(libs.jline.builtins)

  // SQLite Engine
  if (enableSqlite) {
    implementation(projects.packages.sqlite)
  }
  if (enableNativeCryptoV2) {
    api(project(":packages:tcnative"))
  } else {
    implementation(libs.netty.tcnative)
  }

  // Tooling
  implementation(projects.packages.builder)
  implementation(libs.bundles.maven.model)
  implementation(libs.bundles.maven.resolver)
  implementation(libs.mcp)

  // GraalVM: Engines
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmTs)
  implementation(projects.packages.graalvmWasm)
  api(libs.graalvm.polyglot)
  api(libs.graalvm.js.language)
  compileOnly(libs.graalvm.svm)
  nativeImageCompileOnly(libs.javax.inject)

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
      implementation(projects.packages.graalvmKt)
    } else {
      compileOnly(projects.packages.graalvmJvm)
      compileOnly(projects.packages.graalvmJava)
      compileOnly(projects.packages.graalvmKt)
    }
  }

  // GraalVM: Tooling
  implementation(libs.graalvm.tools.dap)
  implementation(libs.graalvm.tools.chromeinspector)
  implementation(libs.graalvm.tools.profiler)
  implementation(libs.graalvm.tools.lsp.api)
  implementation(libs.graalvm.tools.lsp.impl)

  // TODO: patched
  // implementation(libs.graalvm.tools.coverage)

  // Secrets
  implementation(projects.packages.secrets)

  // KotlinX
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.html)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.ktoml)

  // Ktor
  implementation(libs.bundles.ktor.server)
  implementation(libs.ktor.server.cio)
  implementation(libs.ktor.server.netty)

  // Logging
  api(libs.slf4j)
  api(libs.slf4j.jul)

  // General
  implementation(libs.smartexception)
  implementation(libs.minifyHtml)
  implementation(libs.magicProgress)
  implementation(libs.inquirer) {
    exclude(group = "org.jline", module = "jline")
    exclude(group = "org.fusesource.jansi", module = "jansi")
  }

  runtimeOnly(mn.micronaut.graal)
  implementation(mn.netty.handler)

  // we have to include _some_ support for JNA in order to support oshi et al.
  implementation(libs.jna)
  if (enableJnaStatic) {
    implementation(libs.jna.graalvm)
  }

  // Jib
  implementation(libs.jib.core)
  implementation(libs.jib.extension.commons)

  // Tests
  testImplementation(libs.kotlin.test.junit5)
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
  maxHeapSize = "8g"
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
val kotlinHomeRoot = layout.buildDirectory.dir("kotlin-resources/kotlin")

val stagedNativeArgs: List<String> = listOfNotNull(
  "-H:+RemoveUnusedSymbols",
  "-H:+ParseRuntimeOptions",
  "-H:+TrackPrimitiveValues",
  "-H:+UsePredicates",
  "-H:+AllowUnsafeAllocationOfAllInstantiatedTypes",  // fix: oracle/graal#10912
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
  "elide.runtime.feature.js.FetchFeature",
  "elide.tool.feature.LocalInferenceFeature",
  "elide.runtime.feature.engine.NativeTraceFeature",
  "elide.tool.feature.DisallowedHostJvmFeature",
  "elide.tool.feature.FileSystemsFeature",
  "elide.tool.feature.WebBuilderFeature",
  "elide.tool.feature.MediaBuilderFeature",
  onlyIf(enablePython, "elide.runtime.feature.python.PythonFeature"),
  onlyIf(enableNativeTransportV2, "elide.runtime.feature.engine.NativeTransportFeature"),
  onlyIf(enableNativeCryptoV2, "elide.runtime.feature.engine.NativeCryptoFeature"),
  onlyIf(oracleGvm && oracleGvmLibs, "com.oracle.svm.enterprise.truffle.EnterpriseTruffleFeature"),
  onlyIf(oracleGvm && oracleGvmLibs, "com.oracle.truffle.runtime.enterprise.EnableEnterpriseFeature"),
  onlyIf(oracleGvm && enableExperimental, "com.oracle.svm.enterprise.truffle.PolyglotIsolateGuestFeature"),
  onlyIf(oracleGvm && enableExperimental, "com.oracle.svm.enterprise.truffle.PolyglotIsolateHostFeature"),
  onlyIf(enableJnaStatic && HostManager.hostIsMac, "com.sun.jna.SubstrateStaticJNA"),
  onlyIf(enableSqlite, "elide.runtime.feature.engine.NativeSQLiteFeature"),
  onlyIf(enableKotlin, "elide.runtime.gvm.kotlin.feature.KotlinCompilerFeature"),
  onlyIf(enableEmbeddedSvm, "com.oracle.svm.driver.APIOptionFeature"),
)

val enabledSecurityProviders = listOfNotNull(
  onlyIf(enableBouncycastle, "org.bouncycastle.jce.provider.BouncyCastleProvider"),
  onlyIf(enableConscrypt, "org.conscrypt.OpenSSLProvider"),
)

val preinitializedContexts = if (!enablePreinit) emptyList() else listOfNotNull(
  "js",
  onlyIf(enablePreinitializeAll && enablePython, "python"),
  onlyIf(enablePreinitializeAll && enableRuby, "ruby"),
  onlyIf(enablePreinitializeAll && enableJvm && !HostManager.hostIsMac, "java"),
)

val macOsxPlatform = "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform"

val experimentalLlvmEdgeArgs = listOfNotNull(
  "-H:-CheckToolchain",
).plus(listOfNotNull(
  "-fuse-ld=lld",
  "-I$macOsxPlatform/Developer/SDKs/MacOSX.sdk",
  "-I$macOsxPlatform/Developer/SDKs/MacOSX.sdk/usr/include",
  "-L$macOsxPlatform/Developer/SDKs/MacOSX.sdk/usr/lib",
  "-L$macOsxPlatform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks",
  "-F$macOsxPlatform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks",
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

val preinitClasslist = listOf(
  "elide.runtime.plugins.kotlin.shell.DynamicClassLoader",
  "elide.runtime.plugins.kotlin.shell.GuestClassLoader",
).joinToString(",")

val entryApiHeader: File =
  rootProject.layout.projectDirectory.file("crates/entry/headers/elide-entry.h").asFile

val pluginApiHeader: File =
  rootProject.layout.projectDirectory.file("crates/substrate/headers/elide-plugin.h").asFile

if (!entryApiHeader.exists()) {
  error("Failed to locate entry API header: '$entryApiHeader'")
}
if (!pluginApiHeader.exists()) {
  error("Failed to locate plugin API header: '$pluginApiHeader'")
}

val initializeAtBuildtime: List<String> = listOf(
  "kotlin",
  "kotlinx.atomicfu",
  "org.slf4j",
  "ch.qos.logback",
  "io.micronaut.context.DefaultApplicationContextBuilder",
  "org.fusesource.jansi.io.AnsiOutputStream",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap\$1",
  "elide.tool.cli.AbstractToolCommand\$Companion",
  "elide.tool.cli.Elide\$Companion",
  "com.google.common.base.Equivalence\$Equals",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap\$StrongKeyWeakValueEntry\$Helper",
  "com.sun.tools.javac.resources.compiler",
  "com.sun.tools.javac.resources.javac",
  "sun.tools.jar.resources.jar",
  "sun.awt.resources.awt",
  "elide.tool.cli.Elide",
  "elide.tool.cli.cmd.tool.EmbeddedTool\$Companion",
  "elide.runtime.gvm.internals.sqlite",
  "elide.runtime.gvm.internals.sqlite.SqliteModule",
  "elide.runtime.lang.javascript.JavaScriptPrecompiler",
  "java.sql",
  "org.sqlite",
  "org.pkl.core.runtime.VmLanguageProvider",
  "elide.runtime.lang.typescript",
  "elide.runtime.typescript",
  "elide.runtime.plugins.js.JavaScript",
  "elide.runtime.plugins.js.JavaScript\$Plugin",
  "elide.runtime.plugins.AbstractLanguagePlugin",
  "elide.runtime.plugins.AbstractLanguagePlugin\$Companion",
  "elide.tool.io.RuntimeWorkdirManager",
  "elide.tool.io.RuntimeWorkdirManager\$Companion",
  "elide.tool.err.DefaultErrorHandler",
  "elide.tool.err.DefaultErrorHandler\$Companion",
  "elide.tool.err.DefaultStructuredErrorRecorder",
  "elide.tool.err.DefaultStructuredErrorRecorder\$Companion",
  "elide.runtime.core.internals.graalvm.GraalVMEngine\$Companion",
  "elide.runtime.gvm.intrinsics.BuildTimeIntrinsicsResolver",
).plus(listOf(
  "org.jetbrains.kotlin.config",
  "org.jetbrains.kotlin.config.ApiVersion",
  "org.jetbrains.kotlin.config.LanguageVersion",
  "org.jetbrains.kotlin.config.MavenComparableVersion",
  "org.jetbrains.kotlin.com.intellij.openapi.util.Key",
  "org.jetbrains.kotlin.load.java.ReportLevel",
  "org.jetbrains.kotlin.load.java.ReportLevel\$Companion",
  "org.jetbrains.kotlin.load.java.Jsr305Settings",
  "org.jetbrains.kotlin.load.java.JavaTypeEnhancementState",
  "org.jetbrains.kotlin.load.java.JavaTypeEnhancementState\$Companion\$DEFAULT\$1",
  "org.jetbrains.kotlin.com.intellij.util.containers.IntKeyWeakValueHashMap",
  "org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap",
  "org.jetbrains.kotlin.com.intellij.util.containers.IntKeyWeakValueHashMap\$MyReference",
  "org.eclipse.aether.repository.RemoteRepository",
  "org.eclipse.aether.repository.RepositoryPolicy",
  "org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar",
).onlyIf(enableKotlin)).plus(listOf(
  "com.oracle.svm.driver",
).onlyIf(enableEmbeddedSvm))

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
)

val initializeAtRuntime: List<String> = listOfNotNull(
  onlyIf(!enableSqliteStatic, "org.sqlite.SQLiteJDBCLoader"),
  onlyIf(!enableSqliteStatic, "org.sqlite.core.NativeDB"),
  "org.fusesource.jansi.internal.CLibrary",
  "com.github.ajalt.mordant.rendering.TextStyles",
  "elide.tool.err.ErrPrinter",
  "com.google.protobuf.RuntimeVersion",
  "elide.tool.err.ErrorHandler${'$'}ErrorContext",
  "com.google.protobuf.RuntimeVersion",
  "sun.java2d.pipe.Region",
  "sun.java2d.SurfaceData",
  "sun.rmi.server.Util",
  "sun.awt.X11.XWM",
  "sun.awt.X11.XSystemTrayPeer",
  "sun.awt.X11.XDragAndDropProtocols",
  "com.github.ajalt.mordant.terminal.terminalinterface.ffm.TerminalInterfaceFfmLinux",
  "com.github.ajalt.mordant.internal.MppInternalKt",
  "elide.tool.Environment",
  "elide.tool.Environment\$HostEnv",
  "elide.tool.Environment\$Companion",
  "elide.tool.cli.cmd.tool.jar.JarToolAdapterKt",
  "elide.tool.cli.cmd.tool.kotlinc.KotlinCompilerAdapterKt",
  "elide.tool.cli.cmd.tool.javac.JavaCompilerAdapterKt",
  "elide.tool.cli.cmd.tool.javadoc.JavadocToolAdapterKt",
  "elide.tooling.jvm.JarToolKt",
  "elide.tooling.kotlin.KotlinCompilerKt",
  "elide.tooling.jvm.JavaCompilerKt",
  "elide.tooling.jvm.JavadocToolKt",
  "elide.tooling.kotlin.DetektKt",
  "elide.tooling.gvm.nativeImage.NativeImageDriverKt",
  "elide.tooling.containers.JibDriverKt",
  "org.apache.http.impl.auth.NTLMEngineImpl",
  "elide.exec.Tracing",

  // @TODO switch to built-in brotli
  "org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream",
  "org.apache.commons.compress.compressors.xz.XZCompressorOutputStream",

  // --- JNA + OSHI -----

  "com.sun.jna.Native",
  "com.sun.jna.platform.linux.LibC",
  "com.sun.jna.platform.linux.Udev",
  "oshi.software.os.linux.LinuxOperatingSystem",
  "oshi.hardware.platform.linux",
  "oshi.jna.platform.linux.LinuxLibc",
  "oshi.util.UserGroupInfo",
  "oshi.driver.linux.Lshw",
  "oshi.software.os",
  "oshi.software.os.linux",
  "oshi.util.platform.linux.DevPath",
  "oshi.util.platform.linux.ProcPath",
  "oshi.util.platform.linux.SysPath",
  "com.sun.jna.platform.mac.CoreFoundation",
  "oshi.hardware.platform.mac",
  "oshi.hardware.platform.mac.MacFirmware",
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
  "com.sun.jna.platform.win32",
  "com.sun.jna.platform.win32.WinNT",
  "oshi.util.platform.mac.CFUtil",
  "oshi.util.platform.mac.SmcUtil",
  "oshi.util.platform.mac.SysctlUtil",
  "oshi.jna.platform.mac.CoreFoundation",

  // pkl needs this
  "org.msgpack.core.buffer.DirectBufferAccess",

  // --- JDK Tooling -----

  "elide.tool.cli.cmd.tool.jar.JarTool",
  "elide.tool.cli.cmd.tool.javadoc.JavadocTool",
  "jdk.tools.jlink.internal.Main",
  "jdk.tools.jlink.internal.Utils",
  "jdk.tools.jlink.internal.Main\$JlinkToolProvider",
  "jdk.tools.jlink.internal.plugins.LegalNoticeFilePlugin",
  "jdk.jpackage.internal.JPackageToolProvider",

  // --- JNA -----

  "com.sun.jna.Structure${'$'}FFIType",
  "com.sun.jna.platform.mac.IOKit",
  "com.sun.jna.platform.mac.IOKitUtil",
  "com.sun.jna.platform.mac.SystemB",

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

  "oshi.util.GlobalConfig",
  "oshi.hardware.platform.unix",
  "oshi.hardware.platform.unix.aix",
  "oshi.hardware.platform.unix.freebsd",
  "oshi.hardware.platform.unix.openbsd",
  "oshi.hardware.platform.unix.solaris",
  "oshi.hardware.platform.windows",
  "oshi.software.os.windows",
  "oshi.software.os.windows.WindowsOperatingSystem",
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

  // --- Kotlin (Runtime) -----

  "kotlin.random.AbstractPlatformRandom",
  "kotlin.random.XorWowRandom",
  "kotlin.random.Random",
  "kotlin.random.Random${'$'}Default",
  "kotlin.random.RandomKt",
  "kotlin.random.jdk8.PlatformThreadLocalRandom",
  "kotlin.uuid.SecureRandomHolder",

  // --- Kotlin (SDK) -----

  "org.jetbrains.kotlin.com.intellij.psi.LanguageSubstitutors",
  "org.jetbrains.kotlin.com.intellij.openapi.util.objectTree.ThrowableInterner",
  "org.jetbrains.kotlin.com.intellij.ide.plugins.PluginEnabler",
  "org.jetbrains.kotlin.com.intellij.ide.plugins.DisabledPluginsState",
  "org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.VirtualFileManagerImpl",
  "org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.SharedImplUtil",
  "org.jetbrains.kotlin.com.intellij.util.concurrency.AppScheduledExecutorService\$Holder",
  "org.jetbrains.kotlin.com.intellij.util.CachedValueStabilityChecker",
  "org.jetbrains.kotlin.com.intellij.DynamicBundle\$DynamicBundleInternal",
  "org.jetbrains.kotlin.com.intellij.util.CachedValueLeakChecker",
  "org.jetbrains.kotlin.com.intellij.util.AbstractQuery",
  "org.jetbrains.kotlin.com.intellij.util.ConcurrentLongObjectHashMap",
  "org.jetbrains.kotlin.com.intellij.openapi.progress.impl.CoreProgressManager",
  "org.jetbrains.kotlin.com.intellij.util.containers.ConcurrentIntObjectHashMap",

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
  "io.netty.incubator.channel.uring.IOUring",
  "io.netty.incubator.channel.uring.IOUringEventLoopGroup",
  "io.netty.incubator.channel.uring.Native",
  "io.netty.incubator.channel.uring.LinuxSocket",
  "io.netty.handler.codec.http.HttpObjectEncoder",
  "io.netty.handler.pcap.PcapWriteHandler${'$'}WildcardAddressHolder",
  "io.netty.util.internal.NativeLibraryLoader",
  "io.netty.handler.codec.http.HttpServerExpectContinueHandler",
  "io.netty.handler.codec.http.HttpObjectAggregator",
  "io.netty.buffer.Unpooled",
  "io.netty.buffer.UnpooledByteBufAllocator",
  "io.netty.buffer.EmptyByteBuf",

  // --- Netty: Native Crypto -----

  "io.netty.internal.tcnative.Buffer",
  "io.netty.internal.tcnative.CertificateVerifier",
  "io.netty.internal.tcnative.Library",
  "io.netty.internal.tcnative.SSL",
  "io.netty.internal.tcnative.SSLContext",
  "io.netty.internal.tcnative.SSLSession",
  "io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod",
  "io.netty.internal.tcnative.CertificateCompressionAlgo",
  "io.netty.handler.ssl.BouncyCastleAlpnSslUtils",

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

  "elide.runtime.intrinsics.server.http.netty.IOUringTransport",
  "elide.runtime.gvm.internals.node.process.NodeProcess${'$'}NodeProcessModuleImpl",

  // --- Elide Kotlin -----

  "elide.runtime.gvm.kotlin.KotlinPrecompiler",
  "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
  "elide.runtime.gvm.kotlin.scripting.ScriptWithMavenDepsConfiguration",
  "org.eclipse.sisu.wire.ParameterKeys",
  "nonapi.io.github.classgraph.classpath.SystemJarFinder",
  "org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem",
  "org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem${'$'}Companion",
  "org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder",
  "org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder${'$'}Companion",
  "jdk.internal.jrtfs.SystemImage",
  "jdk.internal.jimage.BasicImageReader",
  "jdk.internal.jimage.ImageReader${'$'}SharedImageReader",

  // --- Java Compiler Accouterments -----

  "com.sun.tools.javac.platform.JDKPlatformProvider",

  // --- Jib -----

  "com.google.cloud.tools.jib.cli.Credentials",
  "com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory",
  "com.google.cloud.tools.jib.plugins.common.DefaultCredentialRetrievers",
)

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntimeTest: List<String> = emptyList()

val gvmJvmHome = requireNotNull(
  System.getenv("GRAALVM_HOME") ?:
  System.getenv("JAVA_HOME") ?:
  System.getProperty("java.home")
)

val jniHeaderPaths = listOf(
  gvmJvmHome.let { "$it/include" },
  gvmJvmHome.let {
    val plat = when {
      HostManager.hostIsLinux -> "linux"
      HostManager.hostIsMac -> "darwin"
      HostManager.hostIsMingw -> "windows"
      else -> error("Unsupported OS for JNI headers: ${System.getProperty("os.name")}")
    }
    "$it/include/$plat"
  }
)

val sharedLibFlags = listOf(
  "--shared",
  "-H:+JNIExportSymbols",
)

val nativeMonitoring = listOfNotNull(
  onlyIf(enableJfr, "jfr"),
  onlyIf(enableNmt, "nmt"),
  onlyIf(enableHeapDump, "heapdump"),
  onlyIf(enableJvmstat, "jvmstat"),
  onlyIf(enableJmx, "jmxserver"),
  onlyIf(enableJmx, "jmxclient"),
  onlyIf(enableJmx, "threaddump"),
).joinToString(",")

val commonNativeArgs = listOfNotNull(
  // Debugging flags:
  // "--verbose",
  // "-H:TempDirectory=/tmp/elide-native",
  // "--trace-object-instantiation=",
  "-H:+UnlockExperimentalVMOptions",
  onlyIf(enableCustomCompiler && !cCompiler.isNullOrEmpty(), "--native-compiler-path=$cCompiler"),
  onlyIf(isDebug, "-H:+JNIVerboseLookupErrors"),
  onlyIf(isDebug, "-H:+JNIEnhancedErrorCodes"),
  onlyIf(!enableJit, "-J-Dtruffle.TruffleRuntime=com.oracle.truffle.api.impl.DefaultTruffleRuntime"),
  onlyIf(enableFfm, "-H:+ForeignAPISupport"),
  onlyIf(dumpPointsTo, "-H:PrintAnalysisCallTreeType=CSV"),
  onlyIf(dumpPointsTo, "-H:+PrintImageObjectTree"),
  onlyIf(enabledFeatures.isNotEmpty(), "--features=${enabledFeatures.joinToString(",")}"),
  // Common flags:
  "-march=$elideBinaryArch",
  "--no-fallback",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  // @TODO breaks with old configs, and new configs can't use the agent.
  // "--exact-reachability-metadata",
  "--enable-url-protocols=http,https,jar,resource",
  "--color=always",
  "--initialize-at-build-time",
  "--link-at-build-time=elide",
  "--link-at-build-time=dev.elide",
  "--link-at-build-time=org.pkl",
  "--link-at-build-time=picocli",
  "--enable-native-access=org.graalvm.truffle,io.netty.common,ALL-UNNAMED",
  onlyIf(nativeMonitoring.isNotEmpty(), "--enable-monitoring=${nativeMonitoring}"),
  "-J--add-exports=jdk.jfr/jdk.jfr.internal=org.graalvm.nativeimage.driver,org.graalvm.nativeimage.builder",
  "-J--add-exports=jdk.jfr/jdk.jfr.internal=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.c=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.hosted.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "-J--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "-J--add-opens=org.graalvm.nativeimage.builder/com.oracle.svm.hosted=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.module=ALL-UNNAMED",
  "-J--add-exports=java.base/jdk.internal.jrtfs=ALL-UNNAMED",
  "-J--add-exports=jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED",
  "-J--add-exports=jdk.graal.compiler/jdk.graal.compiler.util.json=com.oracle.graal.reporter",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=io.netty.common/io.netty.util=org.graalvm.nativeimage.builder",
  "--add-opens=io.netty.common/io.netty.util.internal.svm=org.graalvm.nativeimage.builder",
  "-H:+PreserveFramePointer",
  "-H:+ReportExceptionStackTraces",
  "-H:+AddAllCharsets",
  "-H:-AddAllFileSystemProviders",
  "-H:-IncludeLanguageResources",
  "-H:+CopyLanguageResources",
  "-H:+GenerateEmbeddedResourcesFile",
  "-H:-ReduceImplicitExceptionStackTraceInformation",
  "-H:MaxRuntimeCompileMethods=20000",
  "-H:ExcludeResources=META-INF/native/libumbrella.so",
  "-H:ExcludeResources=META-INF/native/libumbrella.a",
  "-H:ExcludeResources=/linux-aarch64.nativelib",  // provided by our own rust-libs, for `minify-html`
  "-H:ExcludeResources=/linux-x64.nativelib",
  "-H:ExcludeResources=/mac-aarch64.nativelib",
  "-H:ExcludeResources=/mac-x64.nativelib",
  "-H:ExcludeResources=/win-x64.nativelib",
  "-H:ExcludeResources=.*.proto",
  "-H:ExcludeResources=elide/app/.*\\.proto",
  "-H:ExcludeResources=elide/assets/.*\\.proto",
  "-H:ExcludeResources=elide/base/.*\\.proto",
  "-H:ExcludeResources=elide/call/.*\\.proto",
  "-H:ExcludeResources=elide/control/.*\\.proto",
  "-H:ExcludeResources=elide/crypto/.*\\.proto",
  "-H:ExcludeResources=elide/data/.*\\.proto",
  "-H:ExcludeResources=elide/net/.*\\.proto",
  "-H:ExcludeResources=elide/tools/.*\\.proto",
  "-H:ExcludeResources=elide/tools/.*/.*\\.proto",
  "-H:ExcludeResources=elide/std/.*\\.proto",
  "-H:ExcludeResources=elide/structs/.*\\.proto",
  "-H:ExcludeResources=elide/stream/.*\\.proto",
  "-H:ExcludeResources=elide/page/.*\\.proto",
  "-H:ExcludeResources=elide/vfs/.*\\.proto",
  "-H:ExcludeResources=logback.xml",
  "-H:ExcludeResources=META-INF/native/libsqlite.*",
  "-H:ExcludeResources=META-INF/elide/embedded/runtime/js/runtime.current.json",
  "-H:ExcludeResources=META-INF/maven/com.aayushatharva.brotli4j/native-linux-x86_64/pom.xml",
  "-H:ExcludeResources=META-INF/maven/com.aayushatharva.brotli4j/native-linux-x86_64/pom.properties",
  "-H:ExcludeResources=META-INF/native/x86_64-unknown-linux-gnu/libsqlitejdbc.so",
  "-H:ExcludeResources=META-INF/micronaut/io.micronaut.core.graal.GraalReflectionConfigurer/.*",
  "-H:ExcludeResources=META-INF/elide/embedded/runtime/wasm/runtime.json",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/coll/zh.res",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/brkitr/khmerdict.dict",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/coll/ko.res",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/brkitr/burmesedict.dict",
  "-H:ExcludeResources=org/graalvm/shadowed/com/ibm/icu/impl/data/icudt74b/brkitr/thaidict.dict",
  // "-H:ExcludeResources=META-INF/native/libnetty_transport_native_epoll_aarch_64.so",
  // "-H:ExcludeResources=META-INF/native/libnetty_transport_native_io_uring_x86_64.so",
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
  "-Delide.staticUmbrella=true",
  "-Delide.root=$rootPath",
  "-Delide.target=$targetPath",
  "-Delide.natives=$nativesPath",
  "-Delide.natives.entryApiHeader=$entryApiHeader",
  "-Delide.natives.pluginApiHeader=$pluginApiHeader",
  "-Delide.auxCache=$enableAuxCache",
  "-Delide.traceCache=${enableAuxCache && enableAuxCacheTrace}",
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
  "-Dpolyglotimpl.AttachLibraryFailureAction=throw",
  "-Dnetty.default.allocator.max-order=3",
  "-Dnetty.resource-leak-detector-level=DISABLED",
  "-Djansi.eager=false",
  "-Dkotlinx.coroutines.scheduler.core.pool.size=2",
  "-Dkotlinx.coroutines.scheduler.max.pool.size=2",
  "-Dkotlinx.coroutines.scheduler.default.name=ElideDefault",
  "-H:+AllowJRTFileSystem",
  onlyIf(enablePreinit, "-Dpolyglot.image-build-time.PreinitializeContexts=$preinitContextsList"),
  onlyIf(enablePreinit, "-Dpolyglot.image-build-time.PreinitializeContextsWithNative=true"),
  onlyIf(enablePreinit, "-Dpolyglot.image-build-time.PreinitializeAllowExperimentalOptions=true"),
  onlyIf(enablePreinit && enableJvm, "-Dpolyglot.image-build-time.PreInitializationClasslist=$preinitClasslist"),
  onlyIf(enablePgoInstrumentation, "--pgo-instrument"),
  onlyIf(enablePgoSampling, "--pgo-sampling"),
  onlyIf(enableHeapReport, "-H:+BuildReportMappedCodeSizeBreakdown"),
  onlyIf(enableHeapReport, "-H:+TrackNodeSourcePosition"),
).asSequence().plus(
  languagePluginPaths.plus(umbrellaNativesPath).plus(jniHeaderPaths).filter {
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
).plus(
  jpmsSvmArgs.map { "-J$it" }.onlyIf(
    enableEmbeddedSvm
  )
).plus(
  listOf(
    "org.graalvm.nativeimage.builder",
  ).let {
    listOf(
      "--add-modules=${it.joinToString(",")}",
      "--module-path",
    ).plus(
      svmModulePath.joinToString(File.pathSeparator)
    )
  }
).toList()

val debugFlags: List<String> = listOfNotNull(
  "--verbose",
  "-g",
  "-H:+SourceLevelDebug",
  "-H:-DeleteLocalSymbols",
  // "-Dpolyglot.engine.TraceCache=true",
  // "-J-Dpolyglot.engine.TraceCache=true",
  // "-H:+PrintMethodHistogram",
  // "-H:+PrintPointsToStatistics",
  // "-H:+PrintRuntimeCompileMethods",
  // "-H:+ReportPerformedSubstitutions",
).onlyIf(isDebug)

val experimentalFlags = listOf(
  "-H:+SupportContinuations",  // -H:+SupportContinuations is in use, but is not supported together with Truffle JIT...
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

// C compiler flags to be included on Linux only.
val linuxOnlyCFlags: List<String> = listOf(
  "-fstack-clash-protection",
).plus(
  listOf("-fopenmp").onlyIf(!enableStatic)
)

// C compiler flags which are always included.
val commonCFlags: List<String> = listOf(
  "-DELIDE",
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
).plus(
  linuxOnlyCFlags.onlyIf(HostManager.hostIsLinux)
)

// Linker flags which are always included.
val commonLinkerOptions: List<String> = listOf()

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
  "-O$nativeOptMode",
  "-fPIC",
  "-fPIE",
  //"-flto",
).plus(
  listOf("-fuse-linker-plugin").onlyIf(!enableClang && !isClang && !HostManager.hostIsMac && !enableStatic)
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
val profiles: List<String> = listOfNotNull(
  "cli-help.iprof",
  "cli-version.iprof",
  "js-fs.iprof",
  "js-hello.iprof",
  "pkl-eval.iprof",
  onlyIf(enablePython, "py-hello.iprof"),
  onlyIf(enablePython, "py-interop.iprof"),
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

// List of PGO profiles, mapped to the directory above cwd and a subdir, because cwd may be deleted during the build.
val profilesList = profiles.joinToString(",") { "../pgo/$it" }

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
  listOf("--pgo=${profilesList}").onlyIf(enablePgo)
).plus(listOf(
  if (oracleGvm && enableSbom) listOf(
    if (enableSbomStrict) "--enable-sbom=cyclonedx,export,strict" else "--enable-sbom=cyclonedx,export"
  ) else emptyList(),
  if (oracleGvm) gvmReleaseFlags else emptyList(),
).flatten()).toList()

val defaultLocale = "en"

val supportedLocales = listOf(
  "en",
  "en_US",
  "en_GB",
)

val jvmDefs = mutableMapOf(
  "sun.misc.unsafe.memory.access" to "allow",
  "elide.strict" to "true",
  "elide.natives" to nativesPath,
  "elide.root" to rootPath,
  "elide.target" to elideTarget.triple,
  "elide.graalvm.ee" to oracleGvm.toString(),
  "elide.mosaic" to enableMosaic.toString(),
  "elide.staticJni" to enableStaticJni.toString(),
  "elide.targetLibc" to libcTarget,
  "elide.js.vm.enableStreams" to "true",
  "elide.jvm" to enableJvm.toString(),
  "elide.kotlin" to enableKotlin.toString(),
  "elide.kotlin.version" to libs.versions.kotlin.sdk.get(),
  "elide.kotlin.verbose" to "false",
  "elide.nativeTransport.v2" to enableNativeTransportV2.toString(),
  "elide.kotlin.version" to libs.versions.kotlin.sdk.get(),
  "elide.kotlinResources" to kotlinHomeRoot.get().asFile.resolve(libs.versions.kotlin.sdk.get()).absolutePath,
  "elide.gvmResources" to gvmResourcesPath,
  "elide.locales.default" to defaultLocale,
  "elide.locales.supported" to supportedLocales.joinToString(","),
  "jdk.image.use.jvm.map" to "false",
  "jna.library.path" to nativesPath,
  "jna.boot.library.path" to nativesPath,
  "io.netty.noUnsafe" to "false",
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
  "aether.lrm.enhanced.split" to true.toString(),
  "aether.lrm.enhanced.splitRemoteRepository" to true.toString(),
  "aether.dependencyCollector.impl" to "bf",
  "aether.dependencyCollector.bf.skipper" to true.toString(),
  "aether.dependencyCollector.bf.threads" to "8",
  "aether.connector.basic.downstreamThreads" to "16",
  "aether.metadataResolver.threads" to "8",
  "aether.transport.http.reuseConnections" to true.toString(),
  "aether.transport.http.connectionMaxTtl" to "900",
  "aether.transport.http.maxConnectionsPerRoute" to "200",
  "aether.connector.basic.smartChecksums" to true.toString(),
  "aether.connector.basic.persistedChecksums" to true.toString(),
  "aether.dependencyCollector.pool.artifact" to "hard",
  "aether.dependencyCollector.pool.dependency" to "hard",
  "aether.dependencyCollector.pool.descriptor" to "hard",
  "polyglot.js.esm-eval-returns-exports" to "true",
  // "java.util.concurrent.ForkJoinPool.common.threadFactory" to "",
  // "java.util.concurrent.ForkJoinPool.common.exceptionHandler" to "",
)

findProperty("elide.logLevel")?.let {
  jvmDefs["elide.logging.root.level"] = it as String
}

val localeArgs = buildList {
  add("-H:DefaultLocale=$defaultLocale")

  if (enableLocaleSupport) {
    add("-H:+UseSystemLocale")
    if (enableAllLocales) {
      add("-H:+IncludeAllLocales")
    }
  } else {
    add("-H:-UseSystemLocale")
    add("-H:-IncludeAllLocales")
  }
}

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to supportedLocales.joinToString(","),
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
  "--gc=$effectiveGc",
  "-R:MaximumHeapSizePercent=80",
).plus(if (oracleGvm) listOf(
  "-Delide.vm.engine.preinitialize=true",
) else listOf(
  "-Delide.vm.engine.preinitialize=false",
)).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx48g",
) else emptyList())).plus(if (oracleGvm) listOf(
  // disabled on windows
  "-H:-AuxiliaryEngineCache",
) else emptyList())

val darwinOnlyArgs = defaultPlatformArgs.plus(listOfNotNull(
  "--gc=$effectiveGc",
  "-R:MaximumHeapSizePercent=80",
  "--initialize-at-build-time=sun.awt.resources.awtosx",
  "-H:NativeLinkerOption=-flto",
  "-H:NativeLinkerOption=$nativesPath/libdiag.a",
  "-H:NativeLinkerOption=$nativesPath/libsqlitejdbc.a",
  "-H:NativeLinkerOption=$nativesPath/libumbrella.a",
  "-H:NativeLinkerOption=$nativesPath/libjs.a",
  "-H:NativeLinkerOption=$nativesPath/libposix.a",
  "-H:NativeLinkerOption=$nativesPath/libtrace.a",
  "-H:NativeLinkerOption=$nativesPath/libexec.a",
  "-H:NativeLinkerOption=$nativesPath/liblocal_ai.a",
  "-H:NativeLinkerOption=$nativesPath/libterminal.a",
  "-H:NativeLinkerOption=$nativesPath/libsubstrate.a",
  "-H:NativeLinkerOption=$nativesPath/libweb.a",
  onlyIf(enableNativeTransportV2, "-H:NativeLinkerOption=$nativesPath/libtransport.a"),
  "-H:NativeLinkerOption=-lm",
  "-H:NativeLinkerOption=-lstdc++",
).plus(if (oracleGvm) listOf(
  "-Delide.vm.engine.preinitialize=true",
) else listOf(
  "-Delide.vm.engine.preinitialize=false",
)).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx64g",
  "--parallelism=12",
))).plus(if (oracleGvm && enableAuxCache) listOf(
  "-H:+AuxiliaryEngineCache",
) else emptyList())

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.toList()

val muslHome = System.getenv("MUSL_HOME") ?: "/opt/musl/1.2.5/lib"

val linuxOnlyArgs = defaultPlatformArgs.plus(
  listOfNotNull(
    "-g",  // always generate debug info on linux
    "-H:NativeLinkerOption=-flto",
    "-H:NativeLinkerOption=-Wl,--gc-sections",
    "-H:NativeLinkerOption=-Wl,--emit-relocs",
    "-H:NativeLinkerOption=$nativesPath/libdiag.a",
    "-H:NativeLinkerOption=$nativesPath/libsqlitejdbc.a",
    "-H:NativeLinkerOption=$nativesPath/libumbrella.a",
    "-H:NativeLinkerOption=$nativesPath/libjs.a",
    "-H:NativeLinkerOption=$nativesPath/libposix.a",
    "-H:NativeLinkerOption=$nativesPath/libtrace.a",
    "-H:NativeLinkerOption=$nativesPath/libexec.a",
    "-H:NativeLinkerOption=$nativesPath/liblocal_ai.a",
    "-H:NativeLinkerOption=$nativesPath/libterminal.a",
    "-H:NativeLinkerOption=$nativesPath/libsubstrate.a",
    "-H:NativeLinkerOption=$nativesPath/libweb.a",
    onlyIf(enableNativeTransportV2, "-H:NativeLinkerOption=$nativesPath/libtransport.a"),
    "-H:NativeLinkerOption=-lm",
    "-H:ExcludeResources=.*dylib",
    "-H:ExcludeResources=.*jnilib",
    "-H:ExcludeResources=.*dll",
    "-H:ExcludeResources=META-INF/native/libsqlitejdbc-linux-amd64.so",
    "-H:ExcludeResources=lib/osx-aarch64/libbrotli.dylib",
    // "-H:ExcludeResources=META-INF/native/libnetty_transport_native_epoll_x86_64.so",
    "-H:ExcludeResources=META-INF/native/libnetty_transport_native_kqueue_x86_64.jnilib",
    "--initialize-at-run-time=io.netty.channel.kqueue.Native",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventLoop",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueEventArray",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueue",
    "--initialize-at-run-time=io.netty.channel.kqueue.KQueueIoHandler",
    "--initialize-at-run-time=io.netty.channel.kqueue.AbstractKQueueChannel",
    "--initialize-at-run-time=io.netty.internal.tcnative.CertificateCompressionAlgo",
  ).plus(
    listOfNotNull(
      onlyIf(enableStatic, "--libc=musl"),
      onlyIf(enableStatic, "-H:NativeLinkerOption=-L$muslHome/lib"),
      onlyIf(enableStatic, "-H:NativeLinkerOption=-lstdc++"),
      onlyIf(enableStatic, "--static"),
      onlyIf(!enableStatic, "--static-nolibc"),
    )
  ).plus(
    listOf(
      "-H:NativeLinkerOption=-lssl",
      "-H:NativeLinkerOption=-lcrypto",
      "-H:NativeLinkerOption=-lstdc++",
      "-H:NativeLinkerOption=-lgomp",
    ).onlyIf(!enableStatic)
  ),
).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:-AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=false",
  ) else listOfNotNull(
    "--gc=$effectiveGc",
    "-R:MaximumHeapSizePercent=80",
    onlyIf(effectiveGc == "serial", "-H:InitialCollectionPolicy=Adaptive"),
  ).plus(if (oracleGvm && enableAuxCache && !enableG1) listOf(
    "-H:+AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=true",
  ) else emptyList())
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx64g",
) else listOf(
  "-J-Xmx64g",
  "--parallelism=32",
))

val linuxGvmReleaseFlags = listOf<String>()

val linuxReleaseArgs = linuxOnlyArgs.plus(
  listOfNotNull(
    onlyIf(!isArm, "-R:+WriteableCodeCache"),
  ).plus(if (oracleGvm) linuxGvmReleaseFlags else emptyList()),
)

val muslArgs = listOf(
  "--static",
  "--libc=musl",
)

val embeddedSubstrateFlags = listOf(
  // @TODO
  // "-H:+GuaranteeSubstrateTypesLinked",
  // "-H:+AllowDeprecatedBuilderClassesOnImageClasspath",
  "-Dcom.oracle.graalvm.isaot=true",
  "-J-Dcom.oracle.graalvm.isaot=true",
  "-Dorg.graalvm.version=25.0.0",
  "-J-Dorg.graalvm.version=25.0.0",
  "--link-at-build-time=com.oracle.svm.driver,com.oracle.svm.driver.metainf",
  "-H:IncludeResources=com/oracle/svm/driver/launcher/.*",
)

val testOnlyArgs: List<String> = emptyList()

val nativeOverrideArgs: List<String> = listOf()

fun nativeCliImageArgs(
  platform: String = "generic",
  target: String = libcTarget,
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
      "linux" ->
        if (target == "musl")
          linuxOnlyArgs.plus(muslArgs)
        else (if (release) linuxReleaseArgs else linuxOnlyArgs)
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
    localeArgs
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
  )).plus(
    sharedLibFlags.onlyIf(sharedLib)
  ).plus(
    embeddedSubstrateFlags.onlyIf(enableEmbeddedSvm)
  ).toList()

graalvmNative {
  testSupport = true
  toolchainDetection = false

  agent {
    defaultMode = findProperty("elide.native.agentMode")?.toString() ?: "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    trackReflectionMetadata = true
    enableExperimentalPredefinedClasses = false
    enableExperimentalUnsafeAllocationTracing = true

    enabled = (
      (System.getenv("GRAALVM_AGENT") == "true" || project.properties.containsKey("agent")) &&
      !gradle.startParameter.isProfile  // profiler for build can't also be active
    )

    modes {
      standard {}
      direct {
        options.addAll(listOf(
          "config-output-dir=${layout.buildDirectory.dir("native/agent-output/direct").get().asFile.absolutePath}",
          "config-write-period-secs=10",
          "config-write-initial-delay-secs=5",
        ))
      }
    }
    metadataCopy {
      inputTaskNames.addAll(listOf("run", "optimizedRun"))
      outputDirectories.add("src/main/resources/META-INF/native-image/dev/elide/elide-cli")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      useArgFile = true
      if (enableToolchains) javaLauncher = gvmLauncher

      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      ).filter {
        // filter out the base jar
        it.name !== tasks.jar.get().outputs.files.filter { it.path.endsWith(".jar") }.single().name
      }

      // compute main compile args
      buildArgs.addAll(nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs))

      buildArgs.addAll(listOf(
        "-Delide.target.buildRoot=${layout.buildDirectory.dir("native/nativeCompile").get().asFile.path}",
      ))
    }

    create("shared") {
      imageName = "libelidemain"
      sharedLibrary = true
      quickBuild = quickbuild
      fallback = false
      useArgFile = true
      if (enableToolchains) javaLauncher = gvmLauncher

      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      ).filter {
        // filter out the base jar
        it.name !== tasks.jar.get().outputs.files.filter { it.path.endsWith(".jar") }.single().name
      }

      buildArgs.addAll(nativeCliImageArgs(
        debug = quickbuild,
        sharedLib = true,
        release = !quickbuild,
        platform = targetOs,
      ))
      buildArgs.add(
        "-Delide.target.buildRoot=${layout.buildDirectory.dir("native/nativeSharedCompile").get().asFile.path}",
      )
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      useArgFile = true
      if (enableToolchains) javaLauncher = gvmLauncher
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true, platform = targetOs))
      classpath = files(
        tasks.optimizedNativeJar,
        configurations.nativeImageClasspath,
        configurations.runtimeClasspath,
      )
      buildArgs.addAll(listOf(
        "-Delide.target.buildRoot=${layout.buildDirectory.dir("native/nativeOptimizedCompile").get().asFile.path}",
      ))
    }

    named("test") {
      imageName = "elide.test"
      fallback = false
      quickBuild = true
      if (enableToolchains) javaLauncher = gvmLauncher
      buildArgs.addAll(nativeCliImageArgs(test = true, platform = targetOs).plus(
        nativeCompileJvmArgs
      ))
      buildArgs.addAll(listOf(
        "-Delide.target.buildRoot=${layout.buildDirectory.dir("native/nativeTestCompile").get().asFile.path}",
        "-Delide.rootDir=${rootProject.layout.projectDirectory.asFile.absolutePath}",
      ))
    }
  }
}

val decompressProfiles: TaskProvider<Copy> by tasks.registering(Copy::class) {
  from(zipTree(layout.projectDirectory.file("profiles.zip")))
  into(layout.buildDirectory.dir("native/pgo"))
  outputs.dir(layout.buildDirectory.dir("native/pgo"))
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

val intermediateKotlinResources = kotlinHomeRoot.map { it.dir(libs.versions.kotlin.sdk.get()) }

// note: the traditional KOTLIN_HOME path does not end with `lib`, so it should point to the versioned kotlin root here,
// even though elide's own paths include `lib`.
val kotlinHomePath: String = kotlinHomeRoot.get()
  .asFile
  .resolve(libs.versions.kotlin.sdk.get())
  .absolutePath

val prepKotlinResources by tasks.registering(Copy::class) {
  from(embeddedKotlin) {
    rename {
      // we need to strip versions from kotlin stdlib and reflect
      val isStdlib = it.startsWith("kotlin-stdlib")
      val isReflect = it.startsWith("kotlin-reflect")
      val isScriptRuntime = it.startsWith("kotlin-script-runtime")
      when {
        isStdlib -> "kotlin-stdlib.jar"
        isReflect -> "kotlin-reflect.jar"
        isScriptRuntime -> "kotlin-script-runtime.jar"
        else -> it
      }
    }
  }
  destinationDir = intermediateKotlinResources.get().dir("lib").asFile
  duplicatesStrategy = EXCLUDE
}

val completionsOut = layout.buildDirectory.dir("cli-completions")
val cliDocsIntermediate = layout.buildDirectory.dir("generated-cli-docs")
val cliDocsOut = layout.buildDirectory.dir("cli-docs")

tasks {
  val generateManpageAsciiDoc by registering(JavaExec::class) {
    dependsOn(classes)
    group = "Documentation"
    description = "Generate CLI asciidoc inputs"
    mainClass = "picocli.codegen.docgen.manpage.ManPageGenerator"

    args(
      commandMain,
      "--outdir=${cliDocsIntermediate.get().asFile.path}",
      "-v",
    )

    classpath = files(
      cliCodeGenerator,
      configurations.compileClasspath,
      configurations.annotationProcessor,
      sourceSets.main.get().runtimeClasspath,
      sourceSets.main.get().output,
    )
  }

  val generateShellCompletions by registering(JavaExec::class) {
    dependsOn(classes)
    group = "Documentation"
    description = "Generate CLI shell completions"
    mainClass = "picocli.AutoComplete"
    workingDir(completionsOut)
    outputs.dir(completionsOut)
    args(commandMain, "--force")

    classpath = files(
      cliCodeGenerator,
      configurations.compileClasspath,
      configurations.annotationProcessor,
      sourceSets.main.get().runtimeClasspath,
      sourceSets.main.get().output,
    )
  }

  asciidoctor {
    dependsOn(generateManpageAsciiDoc)
    sourceDir(cliDocsIntermediate)
    setOutputDir(cliDocsOut)

    outputOptions {
      backends("manpage", "html5")
    }
  }

  val cliDocs by registering {
    group = "Documentation"
    description = "Generate CLI documentation"

    dependsOn(
      generateManpageAsciiDoc,
      asciidoctor,
      generateShellCompletions,
    )
  }

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

  val sampleConfigJson = layout.projectDirectory.file("src/projects/samples.json")

  val allSamples = layout.projectDirectory.dir("src/projects")
    .asFile
    .listFiles()
    .filter { it.isDirectory() }
    .map { it.toPath() to it.name }

  val builtSamples = layout.buildDirectory.dir("packed-samples")

  val allSamplePackTasks = allSamples.map { (path, sample) ->
    register("packSample${sample[0].uppercase()}${sample.substring(1)}", Zip::class) {
      archiveBaseName = sample
      archiveVersion = ""
      destinationDirectory = builtSamples
      from(path) {
        exclude("node_modules", ".dev/artifacts", ".dev/dependencies")
      }
    }
  }

  val packSamples by registering {
    group = "build"
    description = "Package all sample projects embedded with Elide"
    dependsOn(allSamplePackTasks)
  }

  processResources {
    dependsOn(
      ":packages:graalvm:buildRustNativesForHost",
      prepKotlinResources,
      packSamples,
      allSamplePackTasks,
    )
    filterResources()

    from(builtSamples) {
      into("META-INF/elide/samples/")
    }
    from(sampleConfigJson) {
      into("META-INF/elide/samples/")
    }

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
    val defaultWd = rootProject.layout.projectDirectory.asFile
    val runWd = (findProperty("elide.cwd") as? String)?.let {
      Path.of(it).toFile()
    } ?: (findProperty("elide.project") as? String)?.let {
      layout.projectDirectory.dir("src/projects/$it").asFile
    } ?: defaultWd

    workingDir = runWd
    if (enableToolchains) javaLauncher = gvmLauncher

    jvmDefs.forEach {
      systemProperty(it.key, it.value)
    }
    if (findProperty("elide.initLog") == "true") {
      systemProperty("elide.initLog", "true")
    }

    systemProperty(
      "micronaut.environments",
      "dev",
    )
    systemProperty(
      "picocli.ansi",
      "tty",
    )
    environment(
      "KOTLIN_HOME" to kotlinHomePath,
    )
    System.getenv("TERM")?.also {
      val mode = it.substringAfter('-', "16color")
      environment("FORCE_COLOR" to mode)
    }

    System.getenv("COLORTERM")?.also {
      environment("FORCE_COLOR" to it)
    }
    systemProperty(
      "elide.nativeTransport.v2",
      enableNativeTransportV2.toString(),
    )
    jvmDefs.map {
      systemProperty(it.key, it.value)
    }
    // override: kotlin resources may not be built natively yet
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
    ).plus(
      listOf(
        "-verbose:class",
      ).onlyIf(enableVerboseClassLoading)
    ).plus(listOf(
      "-Xms64m",
      "-Xmx4g",
      "-XX:+UnlockExperimentalVMOptions",
      "--enable-native-access=ALL-UNNAMED",
    )))

    standardInput = System.`in`
    standardOutput = System.out

    classpath(
      configurations.compileClasspath,
      configurations.runtimeClasspath,
      jvmOnly,
    )
    jvmArgumentProviders.add(CommandLineArgumentProvider {
      listOf(
        "--module-path=${svmModulePath.joinToString(File.pathSeparator)}",
        "--add-modules=org.graalvm.nativeimage.driver",
      )
    })
  }

  test {
    if (enableToolchains) javaLauncher = gvmLauncher

    // must be root project so that test scripts can be resolved during smoke tests
    workingDir = rootProject.layout.projectDirectory.asFile

    jvmDefs.forEach {
      systemProperty(it.key, it.value)
    }

    systemProperty(
      "elide.rootDir",
      rootProject.layout.projectDirectory.asFile.absolutePath,
    )
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
    environment(
      "KOTLIN_HOME" to kotlinHomePath,
    )
    jvmDefs.map {
      systemProperty(it.key, it.value)
    }

    jvmArgs(jvmModuleArgs.plus(
      listOf(
        "-verbose:class",
      ).onlyIf(enableVerboseClassLoading)
    ).plus(listOf(
      "-Xms64m",
      "-Xmx4g",
      "-XX:+UnlockExperimentalVMOptions",
      "--enable-native-access=ALL-UNNAMED",
      "-Xverify:none",
      // "-agentpath:/.../libprofilerinterface.so=/opt/visualvm/visualvm/lib,5140",
    )))

    standardInput = System.`in`
    standardOutput = System.out

    classpath(
      configurations.compileClasspath,
      configurations.runtimeClasspath,
      jvmOnly,
    )
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
  if (name == svmModulePath.name) {
    return@all
  }
  resolutionStrategy.dependencySubstitution {
    substitute(module("${jsGroup}:${jsName}")).apply {
      using(project(":packages:graalvm-js"))
      because("Uses Elide's patched version of GraalJs")
    }
  }
}

artifacts {
  add(cliNativeOptimized.name, tasks.optimizedNativeJar)
  add(cliJitOptimized.name, tasks.optimizedJitJar)
}

listOf(
  tasks.withType<BuildNativeImageTask>(),
  tasks.withType<GenerateResourcesConfigFile>(),
).forEach {
  it.configureEach {
    dependsOn(
      ":packages:graalvm:natives",
    )
    notCompatibleWithConfigurationCache("insanely broken")
  }
}

fun spawnEmbeddedJvmCopy(receiver: BuildNativeImageTask): Copy {
  val outDir = layout.buildDirectory.dir("native/${receiver.name}/jvm")
    .get()
    .asFile
    .absolutePath
  val jlinkDir = project(":packages:graalvm-jvm").layout.buildDirectory.dir("jlink")
    .get()
    .asFile
    .absolutePath

  return tasks.register("${receiver.name}EmbeddedJvmCopy", Copy::class) {
    dependsOn(":packages:graalvm-jvm:jvmBundle", ":packages:graalvm-jvm:copyNativeImageBuilder")
    from(jlinkDir)
    into(outDir)
  }.get()
}

fun spawnNativeLibCopy(receiver: BuildNativeImageTask): Copy {
  val outDir = layout.buildDirectory.dir("native/${receiver.name}")
    .get()
    .asFile
    .absolutePath
  return tasks.register("${receiver.name}CopyNativeLibs", Copy::class) {
    dependsOn(":packages:graalvm:natives")
    from(nativesPath) {
      include("*.so", "*.dylib", "*.jnilib", "*.dll")
    }
    into(outDir)
  }.get()
}

fun spawnEmbeddedSvmCopy(receiver: BuildNativeImageTask): Copy {
  val outDir = layout.buildDirectory.dir("native/${receiver.name}")
    .get()
    .asFile
    .resolve("resources")
    .resolve("gvm")
    .resolve("lib")
    .absolutePath

  val graalvmHome = System.getenv("GRAALVM_HOME") ?: System.getenv("JAVA_HOME")
    ?: "/usr/lib/jvm/gvm.jdk25"  // default
  val graalvmLib = File(graalvmHome, "lib")

  return tasks.register("${receiver.name}CopyEmbeddedSvm", Copy::class) {
    from(graalvmLib) {
      include(
        "ct.sym",
        "modules",
        "jrt-fs.jar",
        "**/truffle/**/*.jar",
        "**/svm/**/*.jar",
        "**/svm/clibraries/*/**/*.*",
        "**/macros/**",
      )
      exclude(
        "**/svm-wasm/**",
        "**/profile_inference/**",
      )
      eachFile { logger.lifecycle("Copying: ${file.path}") }
    }
    into(outDir)
    includeEmptyDirs = false
  }.get()
}

fun spawnEmbeddedKotlinCopy(receiver: BuildNativeImageTask): Copy {
  val outDir = layout.buildDirectory.dir("native/${receiver.name}")
    .get()
    .asFile
    .resolve("resources")
    .resolve("kotlin")
    .resolve(libs.versions.kotlin.sdk.get())
    .absolutePath

  val runningSet = TreeSet<String>()
  return tasks.register("${receiver.name}CopyEmbeddedKotlin", Copy::class) {
    dependsOn(
      prepKotlinResources,
      ":packages:graalvm-kt:prepKotlinResources"
    )
    from(intermediateKotlinResources) {
      rename {
        if (it in runningSet) {
          error("Already saw: $it")
        } else {
          // we need to strip versions from kotlin stdlib and reflect
          val isStdlib = it.startsWith("kotlin-stdlib")
          val isReflect = it.startsWith("kotlin-reflect")
          val isScriptRuntime = it.startsWith("kotlin-script-runtime")
          when {
            isStdlib -> "kotlin-stdlib.jar"
            isReflect -> "kotlin-reflect.jar"
            isScriptRuntime -> "kotlin-script-runtime.jar"
            else -> it
          }
        }
      }
    }
    into(outDir)
  }.get()
}

// Alpine needs a `resources.tgz` file to be built, which is unpacked upon installation from an apk.
fun spawnResourcesTgzBuilder(receiver: BuildNativeImageTask): Task {
  val nativeOutDir = layout.buildDirectory.dir("native/${receiver.name}")
  val tarFileName = "resources.tar"
  val tarFile = layout.buildDirectory.file("native/${receiver.name}/$tarFileName")
  val tgzFileName = "resources.tgz"
  val tgzFile = layout.buildDirectory.file("native/${receiver.name}/$tgzFileName")

  val tarTask = tasks.register("${receiver.name}ResourcesTar", Tar::class) {
    group = "build"
    description = "Build a tarball of Native Image resources for task '${receiver.name}'"
    archiveFileName.set(tarFileName)
    destinationDirectory.set(nativeOutDir)
    outputs.file(tarFile)
    compression = Compression.NONE
    from(layout.buildDirectory.dir("native/${receiver.name}/resources"))
    dependsOn(
      receiver,
      tasks.named("${receiver.name}CopyEmbeddedKotlin"),
      tasks.named("${receiver.name}CopyEmbeddedSvm"),
      tasks.named("${receiver.name}CopyTopLevelDoc"),
    )
  }
  val tgzTask = tasks.register("${receiver.name}ResourcesTgz", Exec::class) {
    dependsOn(receiver, tarTask)
    commandLine("gzip", "--force", "--best", "--verbose", tarFile.get().asFile.absolutePath)
  }.get()

  val tarball = tasks.register("${receiver.name}BuildResourcesTarball", Exec::class) {
    group = "build"
    description = "Build Native Image apk resources tarball for task '${receiver.name}'"
    dependsOn(tarTask, tgzTask)
    commandLine(
      "mv",
      tarFile.get().asFile.absolutePath + ".gz",
      tgzFile.get().asFile.absolutePath,
    )
  }.get()

  val cleanTar = tasks.register("${receiver.name}RemoveTar", Delete::class) {
    delete(tarFile)
  }

  return tasks.register("${receiver.name}BuildPackageResources") {
    group = "build"
    description = "Build Native Image resources tarball for task '${receiver.name}'"
    dependsOn(tarball, tgzTask)
    finalizedBy(cleanTar)
  }.get()
}

// CLI docs and completions must be copied to the final native image out-dir, so they can be included in package dists.
fun spawnCliCopyDocsAndCompletions(receiver: BuildNativeImageTask): Task {
  val copyManpages = tasks.register("${receiver.name}CopyManpages", Copy::class) {
    enabled = enableCliDocs
    from(cliDocsOut) {
      include("manpage/**/*.*")
    }
    into(layout.buildDirectory.dir("native/${receiver.name}/doc/man"))
  }
  val copyTopLevelDoc = tasks.register("${receiver.name}CopyTopLevelDoc", Copy::class) {
    enabled = enableCliDocs
    from(layout.projectDirectory.dir("packaging/content"))
    into(layout.buildDirectory.dir("native/${receiver.name}"))
  }
  val copyCompletions = tasks.register("${receiver.name}CopyCompletions", Copy::class) {
    enabled = enableCliDocs
    from(completionsOut) {
      include("**/*")
    }
    into(layout.buildDirectory.dir("native/${receiver.name}/completions"))
  }
  return tasks.register("${receiver.name}CopyCliExtras") {
    group = "build"
    description = "Copy CLI docs and completions for task '${receiver.name}'"
    dependsOn(copyTopLevelDoc, copyManpages, copyCompletions)
  }.get()
}

// We build a zip of the `sources/` folder, `elide.debug` symbols, and `gdb-debughelpers.py`.
fun spawnSourcesZipBuilder(receiver: BuildNativeImageTask): Task {
  val srcsPath = layout.buildDirectory.dir("native/${receiver.name}/sources")
  return tasks.register("${receiver.name}SourcesZip", Zip::class) {
    group = "build"
    description = "Build a zip of sources and debug symbols for task '${receiver.name}'"
    archiveFileName.set("sources.zip")
    destinationDirectory.set(layout.buildDirectory.dir("native/${receiver.name}"))
    onlyIf { srcsPath.get().asFile.exists() }
    dependsOn(
      receiver,
      tasks.named("${receiver.name}CopyTopLevelDoc"),
    )

    from(srcsPath) {
      into("sources")
    }
    from(
      layout.buildDirectory.file("native/${receiver.name}/elide.debug"),
      layout.buildDirectory.file("native/${receiver.name}/gdb-debughelpers.py"),
    )
  }.get()
}

fun Task.configureFinalizer(receiver: BuildNativeImageTask) {
  group = "build"
  description = "Finalize Native Image resources for task '${receiver.name}'"
  dependsOn(receiver.name)
}

fun BuildNativeImageTask.createFinalizer() = this@createFinalizer.let { that ->
  buildList<Task> {
    add(spawnCliCopyDocsAndCompletions(that))
    add(spawnResourcesTgzBuilder(that))
    add(spawnSourcesZipBuilder(that))
    if (enableEmbeddedJvm) add(spawnEmbeddedJvmCopy(that))
    if (!enableStaticJni) add(spawnNativeLibCopy(that))
    if (enableKotlin) add(spawnEmbeddedKotlinCopy(that))
    if (enableEmbeddedSvm) add(spawnEmbeddedSvmCopy(that))
  }.let { finalizations ->
    if (finalizations.isNotEmpty()) {
      val finalizer = tasks.register("${name}Finalize") {
        configureFinalizer(this@createFinalizer)
        dependsOn(finalizations.map { it.name })
      }
      finalizedBy(finalizations.map { it.name })
      finalizedBy(finalizer.name)
    }
  }
}

tasks.withType<BuildNativeImageTask>().all {
  createFinalizer()
}

tasks.installDist.configure {
  duplicatesStrategy = DuplicatesStrategy.WARN
}

tasks.prepareJitOptimizations.configure {
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}

tasks.prepareNativeOptimizations.configure {
  jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}

tasks.assemble.configure {
  if (enableCliDocs) dependsOn(tasks.named("cliDocs"))
}
