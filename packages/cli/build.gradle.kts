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

@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import com.jakewharton.mosaic.gradle.MosaicExtension
import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.docker.DockerBuildStrategy
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.crypto.checksum.Checksum
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import elide.internal.conventions.elide
import elide.internal.conventions.kotlin.KotlinTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  java
  `java-library`
  distribution
  publishing
  jacoco
  `jvm-test-suite`
  `maven-publish`

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("io.micronaut.docker")
  alias(libs.plugins.kover)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.micronaut.application)
  alias(libs.plugins.micronaut.graalvm)
  alias(libs.plugins.micronaut.aot)
  alias(libs.plugins.shadow)
  alias(libs.plugins.gradle.checksum)

  id("elide.internal.conventions")
}

elide {
  kotlin {
    target = KotlinTarget.JVM
  }

  docker {
    useGoogleCredentials = true
  }

  jvm {
    alignVersions = false
  }

  java {
    configureModularity = false
  }
}

// Flags affecting this build script:
//
// - `elide.release`: true/false
// - `elide.buildMode`: `dev`, `release`, etc
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

val entrypoint = "elide.tool.cli.ElideTool"

val oracleGvm = false
val enableEdge = true
val enableWasm = true
val enablePython = true
val enableRuby = true
val enableTools = true
val enableMosaic = true
val enableProguard = false
val enableLlvm = false
val enableEspresso = true
val enableExperimental = false
val enableEmbeddedResources = enableExperimental
val enableJpms = enableExperimental
val enableDashboard = oracleGvm
val enableG1 = oracleGvm
val enablePgo = oracleGvm && isRelease
val enablePgoInstrumentation = oracleGvm && !isRelease
val enableSbom = oracleGvm && enableTools
val enableTruffleJson = enableEdge
val encloseSdk = !System.getProperty("java.vm.version").contains("jvmci")
val globalExclusions = emptyList<Pair<String, String>>()

val moduleExclusions = listOf(
  "io.micronaut" to "micronaut-core-processor",
)

buildscript {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
    maven("https://elide.pkg.st/")
  }
  dependencies {
    classpath(libs.plugin.proguard)
    classpath(libs.plugin.mosaic)
  }
}

if (enableMosaic) apply(plugin = "com.jakewharton.mosaic")

val jvmCompileArgs = listOf(
  "--enable-preview",
  "--add-exports=java.base/jdk.internal.module=elide.cli",
  "--add-exports=jdk.internal.vm.compiler/org.graalvm.compiler.options=elide.cli",
  "--add-exports=jdk.internal.vm.compiler/org.graalvm.compiler.options=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.base/com.oracle.svm.util=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=elide.cli",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jdk=ALL-UNNAMED",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.jni=ALL-UNNAMED",
  "--add-reads=elide.graalvm=ALL-UNNAMED",
  "--add-reads=elide.cli=ALL-UNNAMED",
)

val jvmRuntimeArgs = emptyList<String>()

val nativeCompileJvmArgs = jvmCompileArgs.map {
  "-J$it"
}

val jvmModuleArgs = listOf(
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
).plus(jvmCompileArgs).plus(jvmRuntimeArgs)

val ktCompilerArgs = listOf(
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",

  // Fix: Suppress Kotlin version compatibility check for Compose plugin (applied by Mosaic)
  "-P=plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.20-Beta",
)

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
  if (enableJpms) modularity.inferModulePath = true
}

ktlint {
  debug = false
  verbose = false
  android = false
  outputToConsole = false
  ignoreFailures = true
  enableExperimentalRules = true

  filter {
    exclude("elide/tool/cli/ToolTypealiases.kt")
  }
}

kapt {
  useBuildCache = true
  includeCompileClasspath = true
  strictMode = true
  correctErrorTypes = true
}

kotlin {
  target.compilations.all {
    kotlinOptions {
      allWarningsAsErrors = false
      freeCompilerArgs = freeCompilerArgs.plus(ktCompilerArgs).toSortedSet().toList()
    }
  }
}

sourceSets {
  val main by getting {
    if (enableJpms) java.srcDirs(
      layout.projectDirectory.dir("src/main/java9")
    )
  }
}

// use consistent compose plugin version
the<MosaicExtension>().kotlinCompilerPlugin =
  libs.versions.compose.get()

val stamp = (project.properties["elide.stamp"] as? String ?: "false").toBooleanStrictOrNull() ?: false

buildConfig {
  className("ElideCLITool")
  packageName("elide.tool.cli.cfg")
  useKotlinOutput()

  val cliVersion = if (stamp) {
    libs.versions.elide.asProvider().get()
  } else {
    "1.0-dev-${System.currentTimeMillis() / 1000 / 60 / 60 / 24}"
  }
  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"$cliVersion\"")
}

val modules: Configuration by configurations.creating

val classpathExtras: Configuration by configurations.creating {
  extendsFrom(configurations.runtimeClasspath.get())
}

dependencies {
  implementation(platform(libs.netty.bom))

  kapt(mn.micronaut.inject.java)
  annotationProcessor(libs.picocli.codegen)
  classpathExtras(mn.micronaut.core.processor)

  api(projects.packages.base)
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.logback)
  implementation(libs.tink)
  implementation(libs.github.api) {
    exclude(group = "org.bouncycastle")
  }
  implementation("com.jakewharton.mosaic:mosaic-runtime:${libs.versions.mosaic.get()}")

  // GraalVM: Engines
  implementation(projects.packages.graalvm)
  if (enableEspresso) implementation(projects.packages.graalvmJvm)
  if (enableLlvm) implementation(projects.packages.graalvmLlvm)
  if (enablePython) implementation(projects.packages.graalvmPy)
  if (enableRuby) implementation(projects.packages.graalvmRb)
  if (enableEspresso) implementation(projects.packages.graalvmKt)
  if (enableWasm) implementation(projects.packages.graalvmWasm)

  api(libs.picocli)
  api(libs.commons.compress)
  api(libs.commons.pool)
  api(libs.semver)
  api(libs.magicProgress)
  api(libs.slf4j)
  api(libs.slf4j.jul)
  api(libs.slf4j.log4j.bridge)

  // Elide
  implementation(projects.packages.core)
  implementation(projects.packages.base)
  implementation(projects.packages.test)
  implementation(projects.packages.runtimeCore)

  implementation(libs.picocli.jline3)
  implementation(libs.picocli.jansi.graalvm)
  implementation(libs.jline.reader)
  implementation(libs.jline.console)
  implementation(libs.jline.terminal.core)
  implementation(libs.jline.terminal.jansi)
  implementation(libs.jline.builtins)
  implementation(libs.jline.graal) {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
  }

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.reactor)

  api(mn.micronaut.inject)
  implementation(mn.micronaut.picocli)
  implementation(mn.micronaut.http)
  implementation(mn.micronaut.http.netty)
  implementation(mn.micronaut.http.client)
  implementation(mn.micronaut.http.server)
  implementation(mn.netty.handler)
  implementation(mn.netty.handler.proxy)
  implementation(mn.netty.codec.http)
  implementation(mn.netty.codec.http2)
  implementation(mn.netty.buffer)
  implementation(mn.netty.incubator.codec.http3)
  implementation(mn.micronaut.websocket)

  runtimeOnly(mn.micronaut.context)
  runtimeOnly(mn.micronaut.kotlin.runtime)

  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  runtimeOnly(projects.packages.proto.protoKotlinx)

  runtimeOnly(mn.micronaut.graal)
  implementation("org.eclipse.jetty.npn:npn-api:8.1.2.v20120308")
  implementation("org.eclipse.jetty.alpn:alpn-api:1.1.3.v20160715")

  implementation(libs.netty.tcnative)

  val arch = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64", "aarch_64" -> "aarch_64"
    else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
  }
  when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> {
      compileOnly(libs.netty.transport.native.epoll)
      compileOnly(libs.netty.transport.native.kqueue)
      implementation(libs.netty.tcnative.boringssl.static)
    }

    Os.isFamily(Os.FAMILY_UNIX) -> {
      when {
        Os.isFamily(Os.FAMILY_MAC) -> {
          compileOnly(libs.netty.transport.native.epoll)
          compileOnly(libs.netty.transport.native.iouring)
          implementation(libs.netty.transport.native.kqueue)
          implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
          implementation(libs.netty.resolver.dns.native.macos)
          implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-$arch") })
        }

        else -> {
          compileOnly(libs.netty.transport.native.kqueue)
          implementation(libs.netty.transport.native.epoll)
          implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-$arch") })
        }
      }
    }

    else -> {}
  }

  // GraalVM: Tools + Compilers
  compileOnly(libs.graalvm.svm)

  if (encloseSdk) {
    compileOnly(libs.graalvm.sdk)
    compileOnly(libs.graalvm.truffle.api)
  }
  compileOnly(libs.graalvm.tools.lsp.api)
  compileOnly(libs.graalvm.truffle.nfi)
  compileOnly(libs.graalvm.truffle.nfi.libffi)

  runtimeOnly(mn.micronaut.runtime)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(mn.micronaut.test.junit5)
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

distributions {
  create("bin") {
    distributionBaseName = "elide"
    distributionClassifier = targetTag

    contents {
      from(
        tasks.nativeOptimizedCompile,
        layout.projectDirectory.dir("packaging/content"),
      )
    }
  }
}

val binDistZip by tasks.getting(Zip::class)
val binDistTar by tasks.getting(Tar::class) {
  compression = Compression.GZIP
}

signing {
  isRequired = properties["enableSigning"] == "true"
  sign(binDistZip, binDistTar)
}

tasks {
  val distributionChecksums by registering(Checksum::class) {
    group = "distribution"
    description = "Generates checksums for the distribution archives"

    dependsOn(
      binDistZip,
      binDistTar,
    )
    inputFiles.setFrom(
      layout.buildDirectory.file("distributions/elide-$version-$targetTag.zip"),
      layout.buildDirectory.file("distributions/elide-$version-$targetTag.tgz"),
    )
    outputDirectory = layout.buildDirectory.dir("distributions")
    appendFileNameToChecksum = true
    checksumAlgorithm = Checksum.Algorithm.SHA256
    outputs.cacheIf { true }
  }

  val signBinDistZip by getting(Sign::class) {
    dependsOn(distributionChecksums)
  }
  val signBinDistTar by getting(Sign::class) {
    dependsOn(distributionChecksums)
  }

  val dist by registering {
    group = "distribution"
    description = "Builds CLI distributions"

    dependsOn(
      // Distribution: Shadow JAR
      shadowJar,

      // Distribution: Optimized Binary
      nativeOptimizedCompile,

      // Distribution: Archives
      assembleDist,
      binDistTar,
      binDistZip,

      // Distribution: Archive Checksums & Signatures
      distributionChecksums,
      signBinDistZip,
      signBinDistTar,
    )
  }
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
    annotations.addAll(listOf(
      "elide.tool.cli.*",
    ))
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
}

/**
 * Build: CLI Native Image
 */

val commonGvmArgs = listOf(
  "-H:+UseCompressedReferences",
  "-H:+BuildReport",
)

val commonNativeArgs = listOf(
  "--language:js",
  "--language:nfi",
  "--language:icu4j",
  "--language:regex",
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--enable-all-security-services",
  "--install-exit-handlers",
  "-H:CStandard=C11",
  "-H:DefaultCharset=UTF-8",
  "-H:+UseContainerSupport",
  "-H:+ReportExceptionStackTraces",
  "-R:MaxDirectMemorySize=256M",
  "-Dpolyglot.image-build-time.PreinitializeContexts=" + listOfNotNull(
    "js",
    if (enablePython) "python" else null,
    if (enableRuby) "ruby" else null,
    if (enableExperimental && enableEspresso) "java" else null,
  ).joinToString(","),
  if (enablePgoInstrumentation) "--pgo-instrument" else null,
).asSequence().plus(if (enableEdge) listOf(
  "-H:+UnlockExperimentalVMOptions",
) else emptyList()).plus(listOfNotNull(
  if (enableEspresso) "--language:java" else null,
  if (enableWasm) "--language:wasm" else null,
  if (enableLlvm) "--language:llvm" else null,
  if (enablePython) "--language:python" else null,
  if (enableRuby) "--language:ruby" else null,
)).plus(if (enableTools) listOf(
  "--tool:chromeinspector",
  "--tool:coverage",
  "--tool:profiler",
  "--tool:lsp",
  "--tool:dap",
) else emptyList()).plus(if (enableEdge) listOfNotNull(
  if (!enableTruffleJson) null else "--language:truffle-json",
) else emptyList()).plus(if (oracleGvm) commonGvmArgs else emptyList()).toList()

val dashboardFlags: List<String> = listOf(
  "-H:DashboardDump=elide-tool",
  "-H:+DashboardAll",
)

val debugFlags: List<String> = listOfNotNull(
  "-march=compatibility",
).plus(
  if (enableDashboard) dashboardFlags else emptyList()
).plus(
  if (HostManager.hostIsLinux) listOf(
    "-g",
  ) else emptyList()
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

  // Not yet supported/causes issues
  "--tool:sandbox",
  "--tool:insight",
  "--tool:insightheap",
)

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
  "-O3",
  "-v",
).plus(if (!enableRuby) listOf(
  "-flto"
) else emptyList())

// PGO profiles to specify in release mode.
val profiles: List<String> = listOf(
  "cli.iprof",
  "serve.iprof",
)

// GVM release flags
val gvmReleaseFlags: List<String> = listOf(
  "-H:+AOTInliner",
  "-H:+AOTAggregateProfiles",
  "-H:+AggressiveColdCodeOptimizations",
  "-H:+VectorizeSIMD",
  "-H:+LSRAOptimization",
  "-H:+MLProfileInference",
  "-H:+BouncyCastleIntrinsics",
  "-R:+BouncyCastleIntrinsics",
  "-H:+VectorPolynomialIntrinsics",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
  "-O3",
  "-H:+LocalizationOptimizedMode",
  "-H:+RemoveUnusedSymbols",
  "-J-Djdk.image.use.jvm.map=false",
).asSequence().plus(releaseCFlags.flatMap {
  listOf(
    "-H:NativeLinkerOption=$it",
    "--native-compiler-options=$it",
  )
}).plus(if (enablePgo) listOf(
  "--pgo=${profiles.joinToString(",")}",
  "-H:CodeSectionLayoutOptimization=ClusterByEdges",
) else emptyList(),
).plus(
  if (enableSbom) listOf("--enable-sbom") else emptyList()
).plus(
  if (enableDashboard) dashboardFlags else emptyList()
).plus(
  if (oracleGvm) gvmReleaseFlags else emptyList()
).toList()

val jvmDefs = mapOf(
  "user.country" to "US",
  "user.language" to "en",
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTime = listOf(
  // Kotlin Core
  "kotlin._Assertions",
  "kotlin.KotlinVersion",
  "kotlin.SafePublicationLazyImpl",
  "kotlin.LazyThreadSafetyMode",
  "kotlin.LazyKt__LazyJVMKt${'$'}WhenMappings",

  // Kotlin Standard Library
  "kotlin.coroutines.ContinuationInterceptor",
  "kotlin.sequences",
  "kotlin.text.Charsets",
  "kotlin.time",
  "kotlin.time.DurationJvmKt",
  "kotlin.time.Duration",
  "kotlin.time.DurationUnit",

  // Kotlin Reflect / JVM Internals
  "kotlin.jvm.internal.CallableReference",
  "kotlin.jvm.internal.Reflection",
  "kotlin.jvm.internal.PropertyReference",
  "kotlin.jvm.internal.PropertyReference1",
  "kotlin.jvm.internal.PropertyReference1Impl",
  "kotlin.reflect.jvm.internal.CachesKt",
  "kotlin.reflect.jvm.internal.CacheByClassKt",
  "kotlin.reflect.jvm.internal.KClassImpl",
  "kotlin.reflect.jvm.internal.KClassImpl${'$'}Data",
  "kotlin.reflect.jvm.internal.KDeclarationContainerImpl",
  "kotlin.reflect.jvm.internal.KDeclarationContainerImpl${'$'}Data",
  "kotlin.reflect.jvm.internal.RuntimeTypeMapper",
  "kotlin.reflect.jvm.internal.impl.builtins.CompanionObjectMapping",
  "kotlin.reflect.jvm.internal.impl.builtins.PrimitiveType",
  "kotlin.reflect.jvm.internal.impl.builtins.StandardNames",
  "kotlin.reflect.jvm.internal.impl.builtins.StandardNames${'$'}FqNames",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}Function",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}KFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}SuspendFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind${'$'}KSuspendFunction",
  "kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap",
  "kotlin.reflect.jvm.internal.impl.descriptors.runtime.structure.ReflectClassUtilKt",
  "kotlin.reflect.jvm.internal.impl.name.FqName",
  "kotlin.reflect.jvm.internal.impl.name.FqNameUnsafe",
  "kotlin.reflect.jvm.internal.impl.name.SpecialNames",
  "kotlin.reflect.jvm.internal.impl.name.StandardClassIds",
  "kotlin.reflect.jvm.internal.impl.name.StandardClassIdsKt",
  "kotlin.reflect.jvm.internal.impl.resolve.jvm.JvmPrimitiveType",

  // KotlinX Modules
  "kotlinx.datetime",
  "kotlinx.io",
  "kotlinx.coroutines",
  "kotlinx.serialization",
  "kotlinx.serialization.internal",

  // KotlinX Serialization + KotlinX JSON
  "kotlinx.serialization.internal.StringSerializer",
  "kotlinx.serialization.modules.SerializersModuleKt",
  "kotlinx.serialization.json.Json",
  "kotlinx.serialization.json.Json${'$'}Default",
  "kotlinx.serialization.json.JsonElementKt",
  "kotlinx.serialization.json.internal.CharArrayPoolBatchSize",
  "kotlinx.serialization.json.internal.StreamingJsonDecoder${'$'}WhenMappings",
  "kotlinx.serialization.json.internal.WriteMode",
  "kotlinx.serialization.json.internal.ArrayPoolsKt",
  "kotlinx.serialization.json.internal.ByteArrayPool8k",

  // Google Commons + Protobuf
  "com.google.protobuf",
  "com.google.common.html.types.Html",
  "com.google.common.jimfs.Feature",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap${'$'}EntrySet",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueEntry${'$'}Helper",
  "com.google.common.collect.MapMakerInternalMap${'$'}1",
  "com.google.common.base.Equivalence${'$'}Equals",

  // SLF4J + Logback
  "ch.qos.logback",
  "org.slf4j.MarkerFactory",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",

  // Encodings, Parsers, Cryptography
  "com.sun.tools.doclint",
  "org.codehaus.stax2.typed.Base64Variants",
  "org.bouncycastle.util.Properties",
  "org.bouncycastle.util.Strings",
  "org.bouncycastle.crypto.macs.HMac",
  "org.bouncycastle.crypto.prng.drbg.Utils",
  "org.bouncycastle.jcajce.provider.drbg.DRBG",
  "org.xml.sax.helpers.LocatorImpl",
  "org.xml.sax.helpers.AttributesImpl",
  "jdk.jshell.Snippet${'$'}SubKind",
  "com.sun.tools.javac.parser.Tokens${'$'}TokenKind",

  // Databasing
  "org.sqlite.util.ProcessRunner",

  // Micronaut
  "io.micronaut.http.util.HttpTypeInformationProvider",
  "io.micronaut.inject.provider.ProviderTypeInformationProvider",
  "io.micronaut.core.async.ReactiveStreamsTypeInformationProvider",

  // --- Elide ------

  // Elide Runtime Core
  "elide.runtime.core.HostPlatform${'$'}Architecture",
  "elide.runtime.core.HostPlatform${'$'}OperatingSystem",

  // Elide VM Internals
  "tools.elide",
  "elide.runtime.gvm",
  "elide.runtime.gvm.internals",
  "elide.runtime.gvm.internals.context",
  "elide.runtime.gvm.internals.intrinsics",
  "elide.runtime.gvm.internals.vfs",
  "elide.runtime.gvm.internals.AbstractGVMScript",
  "elide.runtime.gvm.internals.AbstractVMAdapter",
  "elide.runtime.gvm.internals.IntrinsicsManager",
  "elide.runtime.gvm.internals.VMConditionalMultiProperty",
  "elide.runtime.gvm.internals.VMConditionalProperty",
  "elide.runtime.gvm.internals.VMRuntimeProperty",
  "elide.runtime.gvm.internals.VMStaticProperty",
  "elide.runtime.gvm.internals.GraalVMGuest",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}JVM",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}JAVASCRIPT",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}PYTHON",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}RUBY",
  "elide.runtime.gvm.internals.GraalVMGuest${'$'}WASM",

  // Elide VM Intrinsics: JavaScript
  "elide.runtime.intrinsics",
  "elide.runtime.intrinsics.js",
  "elide.runtime.intrinsics.js.err",
  "elide.runtime.intrinsics.js.express",
  "elide.runtime.intrinsics.js.typed",
  "elide.runtime.gvm.js",
  "elide.runtime.gvm.internals.intrinsics.js",
  "elide.runtime.gvm.internals.intrinsics.js.base64",
  "elide.runtime.gvm.internals.intrinsics.js.console",
  "elide.runtime.gvm.internals.intrinsics.js.crypto",
  "elide.runtime.gvm.internals.intrinsics.js.express",
  "elide.runtime.gvm.internals.intrinsics.js.fetch",
  "elide.runtime.gvm.internals.intrinsics.js.stream",
  "elide.runtime.gvm.internals.intrinsics.js.struct",
  "elide.runtime.gvm.internals.intrinsics.js.typed",
  "elide.runtime.gvm.internals.intrinsics.js.url",
  "elide.runtime.gvm.internals.intrinsics.js.webstreams",

  // Elide Tool Implementations
  "elide.tool.cli.GuestLanguage",
  "elide.tool.cli.cmd",
  "elide.tool.cli.control",
  "elide.tool.cli.err",
  "elide.tool.cli.state",
  "elide.tool.io",
  "elide.tool.testing",

  // Elide Packages: Framework
  "elide.annotations",
  "elide.base",
  "elide.core",
  "elide.core.crypto",
  "elide.core.encoding",
  "elide.core.encoding.base64",
  "elide.core.encoding.hex",
  "elide.core.platform",
  "elide.util",
  "elide.runtime.jvm",

  // Elide Internals: Engine Implementations
  "elide.runtime.gvm.internals.js",
  "elide.runtime.gvm.internals.python",
  "elide.runtime.gvm.internals.ruby",
  "elide.runtime.gvm.internals.jvm",
  "elide.runtime.gvm.internals.wasm",

  // Elide Internals: VM Engines
  "elide.runtime.gvm.internals.AbstractVMEngine",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}NativeImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResources${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResourceBundle${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}NativeImageInfo${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeNativeResourceSignature${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeVFS${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}UniversalImageInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeImageInfo${'$'}UniversalImageInfo${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeArtifact${'$'}${'$'}serializer",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeInfo",
  "elide.runtime.gvm.internals.AbstractVMEngine${'$'}RuntimeInfo${'$'}${'$'}serializer",
)

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
)

val initializeAtRuntime: List<String> = listOf(
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "com.sun.tools.javac.file.Locations",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
  "io.netty.handler.codec.compression.BrotliOptions",
  "io.netty.handler.codec.http.cookie.ServerCookieEncoder",
  "io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator",
  "io.netty.handler.codec.http.HttpObjectEncoder",
  "io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder",
  "kotlin.random.AbstractPlatformRandom",
  "kotlin.random.XorWowRandom",
  "kotlin.random.Random${'$'}Default",
  "kotlin.random.RandomKt",
  "kotlin.random.jdk8.PlatformThreadLocalRandom",
  "kotlin.internal.jdk8.JDK8PlatformImplementations",
  "kotlin.internal.jdk8.JDK8PlatformImplementations${'$'}ReflectSdkVersion",
)

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntime: List<String> = listOf(
  "elide.tool.cli.ElideTool",
  "org.bouncycastle.jcajce.provider.drbg.DRBG$${'$'}Default",
)

val rerunAtRuntimeTest: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())).plus(if (oracleGvm) listOf(
  "-H:-AuxiliaryEngineCache",
) else emptyList())

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else listOf(
  "-J-Xmx24g",
))).plus(if (oracleGvm) listOf(
  "-H:+AuxiliaryEngineCache",
) else emptyList())

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.plus(listOf(
  "-march=native",
))

val linuxOnlyArgs = defaultPlatformArgs.plus(listOf(
  "--static",
  "-march=native",
  "-H:RuntimeCheckedCPUFeatures=AVX,AVX2",
  "-H:+StaticExecutableWithDynamicLibC",
)).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:+UseG1GC",
    "-H:-AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=false",
  ) else listOf(
    "--gc=serial",
    "-Delide.vm.engine.preinitialize=true",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  ).plus(if (oracleGvm) listOf(
    "-H:+AuxiliaryEngineCache",
  ) else emptyList())
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())

val linuxGvmReleaseFlags = listOf(
  "-H:+ObjectInlining",
)

val linuxReleaseArgs = linuxOnlyArgs.plus(listOf(
  "-R:+WriteableCodeCache",
  "-H:+StripDebugInfo",
).plus(if (oracleGvm) linuxGvmReleaseFlags else emptyList()))

val muslArgs = listOf(
  "--libc=musl",
)

val testOnlyArgs: List<String> = emptyList()

val isEnterprise: Boolean = properties["elide.graalvm.variant"] == "ENTERPRISE"

fun nativeCliImageArgs(
  platform: String = "generic",
  target: String = "glibc",
  debug: Boolean = quickbuild,
  test: Boolean = false,
  release: Boolean = (!quickbuild && !test && (properties["elide.release"] == "true" || properties["buildMode"] == "release")),
): List<String> =
  commonNativeArgs.asSequence().plus(
    jvmCompileArgs
  ).plus(
    jvmCompileArgs.map { "-J$it" }
  ).plus(
    initializeAtBuildTime.map { "--initialize-at-build-time=$it" }
  ).plus(
    initializeAtRuntime.map { "--initialize-at-run-time=$it" }
  ).plus(
    rerunAtRuntime.map { "--rerun-class-initialization-at-runtime=$it" }
  ).plus(when (platform) {
    "windows" -> if (release) windowsReleaseArgs else windowsOnlyArgs
    "darwin" -> if (release) darwinReleaseArgs else darwinOnlyArgs
    "linux" -> if (target == "musl") muslArgs else (if (release) linuxReleaseArgs else linuxOnlyArgs)
    else -> defaultPlatformArgs
  }).plus(
    if (test) {
      testOnlyArgs.plus(initializeAtBuildTimeTest.map {
        "--initialize-at-build-time=$it"
      }).plus(initializeAtRuntimeTest.map {
        "--initialize-at-run-time=$it"
      }).plus(rerunAtRuntimeTest.map {
        "--rerun-class-initialization-at-runtime=$it"
      })
    } else {
      emptyList()
    }
  ).plus(
    jvmDefs.map { "-D${it.key}=${it.value}" }
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" }
  ).plus(
    if (debug && !release) debugFlags else if (release) releaseFlags else emptyList()
  ).filterNotNull().toList()

graalvmNative {
  toolchainDetection = false
  testSupport = true

  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    enableExperimentalPredefinedClasses = false
    enableExperimentalUnsafeAllocationTracing = false
    trackReflectionMetadata = true
    enabled = System.getenv("GRAALVM_AGENT") == "true"

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
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      quickBuild = quickbuild
      sharedLibrary = false
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true, platform = targetOs))
      classpath = files(tasks.optimizedNativeJar, configurations.runtimeClasspath)
    }

    named("test") {
      imageName = "elide.test"
      fallback = false
      quickBuild = true
      buildArgs.addAll(nativeCliImageArgs(test = true, platform = targetOs).filter {
        it != "--language:java"  // espresso is not supported in test mode
      }.plus(
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

  val resources: List<String> get() = listOf(
    "META-INF/elide/embedded/runtime/*/*-$tag.*",
  )
}

enum class ElideTarget (override val tag: String) : TargetInfo {
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
  HostManager.hostIsLinux -> ElideTarget.LINUX_AMD64
  HostManager.hostIsMingw -> ElideTarget.WINDOWS_AMD64
  HostManager.hostIsMac -> when (System.getProperty("os.arch")) {
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
      "META-INF/elide/embedded/runtime/*/*-windows*",
      "META-INF/elide/embedded/runtime/*/*-darwin*",
      "META-INF/elide/embedded/runtime/*/*-linux*",
    ) else emptyArray()),
    *(if (!HostManager.hostIsMingw) arrayOf(
      "*.dll",
      "**/*.dll",
      "win",
      "win/*",
      "win/*/*",
      "win/**/*.*",
      "META-INF/elide/embedded/runtime/*/*-windows*",
    ) else emptyArray()),

    *(if (!HostManager.hostIsMac) arrayOf(
      "*.dylib",
      "**/*.dylib",
      "darwin",
      "darwin/*",
      "darwin/*/*",
      "darwin/**/*.*",
      "META-INF/native/osx/*",
      "META-INF/native/*darwin*",
      "META-INF/native/*osx*",
      "META-INF/native/*macos*",
      "META-INF/native/*.dylib",
      "META-INF/elide/embedded/runtime/*/*-darwin*",
    ) else emptyArray()),

    *(if (!HostManager.hostIsLinux) arrayOf(
      "*.so",
      "**/*.so",
      "linux/aarch64/*.so",
      "linux/amd64/*.so",
      "*/epoll/*",
      "META-INF/native/linux64/*",
      "META-INF/native/*linux*",
      "META-INF/native/*.so",
      "META-INF/elide/embedded/runtime/*/*-linux*",
    ) else emptyArray()),

    *(when (targetArch ?: System.getProperty("os.arch")) {
      "aarch64" -> arrayOf(
        "*/x86_64/*",
        "*/amd64/*",
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
    })
  )
}

fun Jar.applyJarSettings() {
  // include collected reachability metadata
  duplicatesStrategy = EXCLUDE
  filterResources(properties["elide.targetArch"] as? String)

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
      "Implementation-Vendor" to "Elide Ventures, LLC",
      "Application-Name" to "Elide",
      "Codebase" to "https://github.com/elide-dev/elide",
    )
  }
}

/**
 * Build: CLI Docker Images
 */

tasks {
  processResources {
    filterResources()
  }

  jar {
    from(collectReachabilityMetadata)
  }

  listOf(
    jar,
    optimizedJitJar,
    optimizedNativeJar,
    shadowJar,
  ).forEach {
    it.configure {
      applyJarSettings()
    }
  }

  compileJava {
    if (enableJpms) {
      options.javaModuleVersion = provider { version as String }
      modularity.inferModulePath = true
    }
  }

  named("run", JavaExec::class).configure {
    systemProperty("micronaut.environments", "dev")
    systemProperty("picocli.ansi", "tty")
    jvmArgs(jvmModuleArgs)

    standardInput = System.`in`
    standardOutput = System.out

    if (enableEdge) {
      javaToolchains {
        javaLauncher.set(
          launcherFor {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.GRAAL_VM
          }
        )
      }
    }
  }

  optimizedRun {
    systemProperty("micronaut.environments", "dev")
    jvmArgs(jvmModuleArgs)
    standardInput = System.`in`
    standardOutput = System.out

    if (enableEdge) {
      javaToolchains {
        javaLauncher.set(
          launcherFor {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.GRAAL_VM
          }
        )
      }
    }
  }

  test {
    if (enableEdge) {
      javaToolchains {
        javaLauncher.set(
          launcherFor {
            languageVersion = JavaLanguageVersion.of(21)
            vendor = JvmVendorSpec.GRAAL_VM
          }
        )
      }
    }
  }

  jar {
    from(collectReachabilityMetadata)
  }

//  withType(com.google.devtools.ksp.gradle.KspTaskJvm::class).configureEach {
//    kotlinOptions {
//      jvmTarget = Elide.kotlinJvmTargetMaximum
//      javaParameters = true
//      languageVersion = Elide.kotlinLanguage
//      apiVersion = Elide.kotlinLanguage
//      allWarningsAsErrors = false
//      freeCompilerArgs = freeCompilerArgs.plus(ktCompilerArgs).toSortedSet().toList()
//    }
//  }

  withType(org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class).configureEach {
    kotlinOptions {
      allWarningsAsErrors = false
      freeCompilerArgs = freeCompilerArgs.plus(ktCompilerArgs).toSortedSet().toList()
    }
  }

  nativeOptimizedCompile {
    dependsOn(decompressProfiles)
  }

  dockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm20:latest"
    buildStrategy = DockerBuildStrategy.DEFAULT
  }

  optimizedDockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm20:latest"
    buildStrategy = DockerBuildStrategy.DEFAULT
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(jvmCompileArgs)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = freeCompilerArgs.plus(ktCompilerArgs).toSortedSet().toList()
    allWarningsAsErrors = false  // module path breaks @TODO: fix
  }
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images = listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/elide/native:latest"
  )
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images = listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/elide/native:opt-latest"
  )
}

// CLI tool is native-only.

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  enabled = false
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  enabled = false
}

configurations.all {
  globalExclusions.forEach {
    exclude(group = it.first, module = it.second)
  }

  if (enableJpms && name != "modules" && name != "classpathExtras") moduleExclusions.forEach {
    exclude(group = it.first, module = it.second)
  }

  resolutionStrategy.dependencySubstitution {
    substitute(module("net.java.dev.jna:jna"))
      .using(module("net.java.dev.jna:jna:${libs.versions.jna.get()}"))
  }

  if (!encloseSdk) {
    // provided by runtime
    exclude(group = "org.graalvm.sdk", module = "graal-sdk")
    exclude(group = "org.graalvm.truffle", module = "truffle-api")
  }
}

// Fix: Java9 Modularity
if (enableJpms) {
  val compileKotlin: KotlinCompile by tasks
  val compileJava: JavaCompile by tasks
  compileKotlin.destinationDirectory.set(compileJava.destinationDirectory)
}

afterEvaluate {
  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    kotlinOptions {
      freeCompilerArgs = freeCompilerArgs.plus(ktCompilerArgs).toSortedSet().toList()
      allWarningsAsErrors = false
    }
  }

  listOf(
    "buildLayers",
    "optimizedBuildLayers",
    "optimizedJitJarAll",
    "shadowJar",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}
