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

import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.docker.DockerBuildStrategy
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.crypto.checksum.Checksum
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import Java9Modularity.configure as configureJava9ModuleInfo

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
  id(libs.plugins.kover.get().pluginId)
  id(libs.plugins.buildConfig.get().pluginId)
  id(libs.plugins.micronaut.application.get().pluginId)
  id(libs.plugins.micronaut.graalvm.get().pluginId)
  id(libs.plugins.micronaut.aot.get().pluginId)
  id(libs.plugins.shadow.get().pluginId)
  id(libs.plugins.gradle.checksum.get().pluginId)
  id("dev.elide.build.docker")
  id("dev.elide.build")
}

group = "dev.elide"
version = rootProject.version as String

val entrypoint = "elide.tool.cli.ElideTool"

val enableEspresso = false
val enableWasm = true
val enableLlvm = false
val enablePython = true
val enableRuby = true
val enableTools = true
val enableSbom = false
val enableG1 = true
val enablePgo = false
val enablePgoInstrumentation = false
val enableMosaic = true
val enableProguard = false
val enableDashboard = false
val encloseSdk = false
val oracleGvm = false

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
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=elide.cli",
  "--add-exports=org.graalvm.nativeimage.builder/com.oracle.svm.core.option=ALL-UNNAMED",
)

val nativeCompileJvmArgs = jvmCompileArgs.map {
  "-J$it"
}

val jvmModuleArgs = listOf(
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
).plus(jvmCompileArgs)

val ktCompilerArgs = listOf(
  "-progressive",
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",
  "-Xjavac-arguments=${jvmCompileArgs.joinToString(",")}}",
)

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
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

kotlin {
  target.compilations.all {
    kotlinOptions {
      jvmTarget = Elide.kotlinJvmTargetMaximum
      javaParameters = true
      languageVersion = Elide.kotlinLanguage
      apiVersion = Elide.kotlinLanguage
      allWarningsAsErrors = false
      freeCompilerArgs = ktCompilerArgs
    }
  }
}

// use consistent compose plugin version
//the<com.jakewharton.mosaic.gradle.MosaicExtension>().kotlinCompilerPlugin =
//  libs.versions.compose.get()

kapt {
  useBuildCache = true
  includeCompileClasspath = false
  strictMode = true
  correctErrorTypes = true
  keepJavacAnnotationProcessors = true
}

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

dependencies {
  implementation(platform(libs.netty.bom))

  kapt(libs.micronaut.inject.java)
  kapt(libs.picocli.codegen)

  api(projects.packages.base)
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.logback)
  implementation(libs.conscrypt)
  implementation(libs.tink)
  implementation("com.jakewharton.mosaic:mosaic-runtime:${libs.versions.mosaic.get()}")

  // GraalVM: Engines
  implementation(projects.packages.graalvm)
  implementation(projects.packages.graalvmJvm)
  implementation(projects.packages.graalvmLlvm)
  implementation(projects.packages.graalvmPy)
  implementation(projects.packages.graalvmRb)
  implementation(projects.packages.graalvmKt)
  implementation(projects.packages.graalvmWasm)

  // GraalVM: Tools + Compilers
  compileOnly(libs.graalvm.svm)

  api(libs.picocli)
  implementation(libs.picocli.jansi.graalvm)
  implementation(libs.slf4j)
  implementation(libs.slf4j.jul)
  implementation(libs.jline.reader)
  implementation(libs.jline.console)
  implementation(libs.jline.terminal.core)
  implementation(libs.jline.terminal.jansi)
  implementation(libs.jline.builtins)
  compileOnly(libs.jline.graal) {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
  }

  implementation(libs.kotlinx.coroutines.core)

  api(libs.micronaut.inject)
  implementation(libs.micronaut.picocli)
  runtimeOnly(libs.micronaut.context)
  runtimeOnly(libs.micronaut.kotlin.runtime)

  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  runtimeOnly(projects.packages.proto.protoKotlinx)

  runtimeOnly(libs.micronaut.graal)

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

  if (encloseSdk) {
    compileOnly(libs.graalvm.sdk)
    compileOnly(libs.graalvm.truffle.api)
  }
  compileOnly(libs.graalvm.tools.lsp.api)
  compileOnly(libs.graalvm.truffle.nfi)
  compileOnly(libs.graalvm.truffle.nfi.libffi)

  runtimeOnly(libs.micronaut.runtime)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.micronaut.test.junit5)
}

application {
  mainClass = entrypoint
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
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
  runtime = MicronautRuntime.NETTY
  enableNativeImage(true)

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.tool.cli.*",
    ))
  }

  aot {
//    configFile = file("$projectDir/aot-native.properties")

    convertYamlToJava = false
    precomputeOperations = false
    cacheEnvironment = false
    deduceEnvironment = false
    replaceLogbackXml = false

    optimizeServiceLoading = false
    optimizeClassLoading = true
    optimizeNetty = false
    possibleEnvironments = listOf("cli")

    netty {
      enabled = false
      machineId = "13-37-7C-D1-6F-F5"
      pid = "1337"
    }
  }
}

tasks.withType(Test::class).configureEach {
  useJUnitPlatform()
  systemProperty("elide.test", "true")
}

tasks.named<JavaExec>("run") {
  systemProperty("micronaut.environments", "dev")
  systemProperty("picocli.ansi", "tty")
  jvmArgs(jvmModuleArgs)
  standardInput = System.`in`
  standardOutput = System.out
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

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
  "--language:truffle-json",
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  "-H:CStandard=C11",
  "-H:DefaultCharset=UTF-8",
  "-H:+UseContainerSupport",
  "-H:+ReportExceptionStackTraces",
  "-H:-EnableAllSecurityServices",
  "-R:MaxDirectMemorySize=256M",
  "-Dpolyglot.image-build-time.PreinitializeContexts=js",
  "--trace-object-instantiation=elide.tool.cli.PropertySourceLoaderFactory",
  if (enablePgoInstrumentation) "--pgo-instrument" else null,
).plus(listOfNotNull(
  if (enableEspresso) "--language:java" else null,
  if (enableWasm) "--language:wasm" else null,
  if (enableLlvm) "--language:llvm" else null,
  if (enablePython) "--language:python" else null,
  if (enableRuby) "--language:ruby" else null,
)).plus(
  if (enableTools) listOf(
    "--tool:chromeinspector",
    "--tool:coverage",
    "--tool:profiler",
  ) else emptyList()
).plus(
  jvmModuleArgs
).plus(if (oracleGvm) commonGvmArgs else emptyList())

val dashboardFlags: List<String> = listOf(
  "-H:DashboardDump=elide-tool",
  "-H:+DashboardAll",
)

val debugFlags: List<String> = listOfNotNull(
  "-g",
  "-march=compatibility",
).plus(
  if (enableDashboard) dashboardFlags else emptyList()
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
  "--tool:lsp",  // Causes crashes
  "--tool:sandbox",
  "--tool:dap",
  "--tool:insight",
  "--tool:insightheap",
)

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
//  "-O3",
//  "-flto",
)

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
  "-H:+MLProfileInference",
  "-H:+BouncyCastleIntrinsics",
  "-R:+BouncyCastleIntrinsics",
  "-H:+VectorPolynomialIntrinsics",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
  "-O2",
//  "-H:+LocalizationOptimizedMode",
//  "-H:+LSRAOptimization",
//  "-H:+RemoveUnusedSymbols",
//  "-J-Djdk.image.use.jvm.map=false",
).plus(releaseCFlags.flatMap {
  listOf(
    "-H:NativeLinkerOption=$it",
    "-H:CCompilerOption=$it",
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
)

val jvmDefs = mapOf(
  "user.country" to "US",
  "user.language" to "en",
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTime = listOf(
  "kotlin.DeprecationLevel",
  "kotlin.annotation.AnnotationRetention",
  "kotlin.coroutines.intrinsics.CoroutineSingletons",
  "kotlin.annotation.AnnotationTarget",
  "com.google.common.jimfs.Feature",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "ch.qos.logback",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",
//  "org.codehaus.stax2.typed.Base64Variants",
//  "org.bouncycastle.util.Properties",
//  "org.bouncycastle.util.Strings",
//  "org.bouncycastle.crypto.macs.HMac",
//  "org.bouncycastle.crypto.prng.drbg.Utils",
//  "org.bouncycastle.jcajce.provider.drbg.DRBG",
//  "org.bouncycastle.jcajce.provider.drbg.DRBG$${'$'}Default",
//  "com.sun.tools.doclint",
//  "jdk.jshell.Snippet${'$'}SubKind",
//  "com.sun.tools.javac.parser.Tokens${'$'}TokenKind",
  "org.xml.sax.helpers.LocatorImpl",
  "org.xml.sax.helpers.AttributesImpl",
  "org.sqlite.util.ProcessRunner",
  "io.netty.handler.codec.http.cookie.ServerCookieEncoder",
  "com.google.common.collect.MapMakerInternalMap",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueSegment",
  "com.google.common.collect.MapMakerInternalMap${'$'}EntrySet",
  "com.google.common.collect.MapMakerInternalMap${'$'}StrongKeyWeakValueEntry${'$'}Helper",
  "com.google.common.collect.MapMakerInternalMap${'$'}1",
  "com.google.common.base.Equivalence${'$'}Equals",

  //
//  "elide.tool.cli.HttpRequestFactoryFactory",
//  "elide.tool.cli.HttpResponseFactoryFactory",
//  "elide.tool.cli.PropertySourceLoaderFactory",
//  "elide.tool.cli.BeanConfigurationFactory",
//  "elide.tool.cli.BeanDefinitionReferenceFactory",
//  "elide.tool.cli.BeanIntrospectionReferenceFactory",
)

val initializeAtBuildTimeTest: List<String> = listOf(
  "org.junit.platform.launcher.core.LauncherConfig",
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
)

val initializeAtRuntime: List<String> = listOf(
  "io.netty.channel.ChannelInitializer",
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
  "com.sun.tools.javac.file.Locations",
  "org.bouncycastle.jcajce.provider.drbg.DRBG${'$'}NonceAndIV",
)

val initializeAtRuntimeTest: List<String> = emptyList()

val rerunAtRuntime: List<String> = emptyList()

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
  "-H:+AllowJRTFileSystem",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList())).plus(if (oracleGvm) listOf(
  "-H:+AuxiliaryEngineCache",
) else emptyList())

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.plus(emptyList())

val linuxOnlyArgs = defaultPlatformArgs.plus(listOf(
  "--static",
  "-march=native",
  "-H:RuntimeCheckedCPUFeatures=AVX,AVX2",
  "-H:+StaticExecutableWithDynamicLibC",
  "-H:+AllowJRTFileSystem",
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

val linuxReleaseArgs = linuxOnlyArgs.plus(listOf(
  "-R:+WriteableCodeCache",
  "-H:+StripDebugInfo",
  "-H:+ObjectInlining",
))

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

  metadataRepository {
    enabled = true
    version = GraalVMVersions.graalvmMetadata
  }

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
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      imageName = if (quickbuild) "elide.debug" else "elide"
      fallback = false
      buildArgs.addAll(nativeCliImageArgs(debug = quickbuild, release = !quickbuild, platform = targetOs))
      quickBuild = quickbuild
      sharedLibrary = false
      systemProperty("picocli.ansi", "tty")
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true, platform = targetOs))
      quickBuild = quickbuild
      sharedLibrary = false
      systemProperty("picocli.ansi", "tty")
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

/**
 * Build: CLI Docker Images
 */

tasks {
  jar {
    from(collectReachabilityMetadata)
  }

//  shadowJar {
//    from(collectReachabilityMetadata)
//
//    exclude(
//      "java-header-style.xml",
//      "license.header",
//    )
//  }

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
    apiVersion = Elide.kotlinLanguageBeta
    languageVersion = Elide.kotlinLanguageBeta
    jvmTarget = Elide.kotlinJvmTargetMaximum
    javaParameters = true
    freeCompilerArgs = ktCompilerArgs
    allWarningsAsErrors = false  // module path breaks @TODO: fix
    incremental = true
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

configureJava9ModuleInfo(project)

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("net.java.dev.jna:jna"))
      .using(module("net.java.dev.jna:jna:${libs.versions.jna.get()}"))
  }

  // provided by runtime
  exclude(group = "org.graalvm.sdk", module = "graal-sdk")
  exclude(group = "org.graalvm.truffle", module = "truffle-api")
}

afterEvaluate {
  tasks.named<JavaExec>("optimizedRun") {
    systemProperty("micronaut.environments", "dev")
    systemProperty("picocli.ansi", "tty")
  }

  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    kotlinOptions {
      apiVersion = Elide.kotlinLanguageBeta
      languageVersion = Elide.kotlinLanguageBeta
      jvmTarget = Elide.kotlinJvmTargetMaximum
      javaParameters = true
      freeCompilerArgs = ktCompilerArgs
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
