@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import Java9Modularity.configureJava9ModuleInfo
import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.docker.DockerBuildStrategy
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
  id("org.jetbrains.kotlinx.kover")
  id("com.github.gmazzo.buildconfig")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.aot")
  id("com.github.johnrengelman.shadow")
  id("dev.elide.build.docker")
  id("dev.elide.build")
}

group = "dev.elide"
version = rootProject.version as String

val entrypoint = "elide.tool.cli.ElideTool"

val enableEspresso = false
val enableWasm = true
val enableLlvm = false
val enablePython = false
val enableRuby = false
val enableSbom = true
val enableG1 = false
val enablePgo = true
val enablePgoInstrumentation = false
val enableMosaic = true
val enableProguard = false
val enableUpx = false

val ktCompilerArgs = listOf(
  "-progressive",
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",

  // Fix: Suppress Kotlin version compatibility check for Compose plugin (applied by Mosaic)
  "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.0"
)

buildscript {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
    maven("https://elide.pkg.st/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  dependencies {
    classpath(libs.plugin.proguard)
    classpath(libs.plugin.mosaic)
  }
}

if (enableMosaic) apply(plugin = "com.jakewharton.mosaic")

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
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

kapt {
  useBuildCache = true
  includeCompileClasspath = false
  strictMode = true
  correctErrorTypes = true
}

buildConfig {
  className("ElideCLITool")
  packageName("elide.tool.cli.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"${libs.versions.elide.asProvider().get()}\"")
}

dependencies {
  implementation(platform(libs.netty.bom))
  api(libs.slf4j)

  kapt(libs.micronaut.inject.java)
  kapt(libs.picocli.codegen)

  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm"))
  implementation(project(":packages:server"))
  implementation(project(":tools:bundler"))
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlin.scripting.common)
  implementation(libs.kotlin.scripting.jvm)
  implementation(libs.kotlin.scripting.jvm.host)
  implementation(libs.logback)

  implementation(libs.picocli)
  implementation(libs.picocli.jansi.graalvm)
  implementation(libs.picocli.jline3)
  implementation(libs.slf4j.jul)
  implementation(libs.jline.all)
  implementation(libs.jline.builtins)
  implementation(libs.jline.graal) {
    exclude(group = "org.slf4j", module = "slf4j-jdk14")
  }

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.picocli)
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.kotlin.extension.functions)
  implementation(libs.micronaut.kotlin.runtime)

  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-protobuf"))
  implementation(project(":packages:proto:proto-kotlinx"))

  // Netty: Native
  implementation(libs.netty.tcnative)
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(libs.netty.transport.native.unixCommon)
  implementation(libs.netty.transport.native.epoll)
  implementation(libs.netty.transport.native.kqueue)

  // Linux
  implementation(libs.netty.transport.native.epoll)
  implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-x86_64") })
  implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-aarch_64") })
  implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-x86_64") })
  implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-aarch_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })

  // macOS/BSD
  implementation(libs.netty.transport.native.kqueue)
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-x86_64") })
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-aarch_64") })
  implementation(libs.netty.resolver.dns.native.macos)

  compileOnly(libs.graalvm.sdk)
  compileOnly(libs.graalvm.espresso.polyglot)
  compileOnly(libs.graalvm.espresso.hotswap)
  compileOnly(libs.graalvm.tools.lsp.api)
  compileOnly(libs.graalvm.truffle.api)
  compileOnly(libs.graalvm.truffle.nfi)
  compileOnly(libs.graalvm.truffle.nfi.libffi)

  runtimeOnly(libs.micronaut.runtime)

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(project(":packages:test"))
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

sonarqube {
  isSkipProject = true
}

val jvmModuleArgs = listOf(
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
)

val targetOs = when {
  Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
  Os.isFamily(Os.FAMILY_MAC) -> "darwin"
  Os.isFamily(Os.FAMILY_UNIX) -> "linux"
  else -> "generic"
}

/**
 * Framework: Micronaut
 */

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = MicronautRuntime.NETTY

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.tool.cli.*",
    ))
  }

  aot {
    version(libs.versions.micronaut.aot.get())
    configFile = file("$projectDir/aot-native.properties")

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

tasks.test {
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

afterEvaluate {
  tasks.named("testNativeImage") {
    enabled = false
  }
}

/**
 * Build: CLI Native Image
 */

val commonNativeArgs = listOf(
  "--language:js",
  "--language:nfi",
  "--language:icu4j",
  "--language:regex",
  "--tool:chromeinspector",
  "--tool:coverage",
  "--tool:lsp",
  "--tool:sandbox",
  "--tool:dap",
  "--tool:insight",
  "--tool:insightheap",
  "--tool:profiler",
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  "-H:CStandard=C11",
  "-H:DefaultCharset=UTF-8",
  "-H:+AuxiliaryEngineCache",
  "-H:+UseContainerSupport",
  "-H:+UseCompressedReferences",
  "-H:+ReportExceptionStackTraces",
  "-R:MaxDirectMemorySize=256M",
  "-Dpolyglot.image-build-time.PreinitializeContexts=js",
  if (enablePgoInstrumentation) "--pgo-instrument" else null,
).plus(listOfNotNull(
  if (enableEspresso) "--language:java" else null,
  if (enableWasm) "--language:wasm" else null,
  if (enableLlvm) "--language:llvm" else null,
  if (enablePython) "--language:python" else null,
  if (enableRuby) "--language:ruby" else null,
)).plus(
  jvmModuleArgs
)

val dashboardFlags = listOf(
  "-H:DashboardDump=elide-tool",
  "-H:+DashboardAll",
)

val debugFlags = listOfNotNull(
  "-g",
  "-march=compatibility",
).plus(dashboardFlags)

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
)

// CFlags for release mode.
val releaseCFlags = listOf(
  "-O3",
  "-flto",
)

// PGO profiles to specify in release mode.
val profiles = listOf(
  "cli.iprof",
  "serve.iprof",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags = listOf(
  "-O2",
  "-dsa",
  "-H:+AOTInliner",
  "-H:+BuildReport",
  "-H:+MLProfileInference",
  "-H:+LocalizationOptimizedMode",
  "-H:+RunMainInNewThread",
  "-H:+BouncyCastleIntrinsics",
  "-H:+VectorPolynomialIntrinsics",
  "-H:+VectorizeSIMD",
  "-H:+AOTAggregateProfiles",
  "-H:+AggressiveColdCodeOptimizations",
  "-H:+LSRAOptimization",
  "-H:+RemoveUnusedSymbols",
  "-R:+BouncyCastleIntrinsics",
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
  "kotlin.annotation.AnnotationTarget",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
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
)

val initializeAtRuntime: List<String> = listOf(
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
)

val rerunAtRuntime: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-R:MaximumHeapSizePercent=80",
  "-H:InitialCollectionPolicy=Adaptive",
  "-XX:-CollectYoungGenerationSeparately",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList()))

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-R:MaximumHeapSizePercent=80",
  "-H:InitialCollectionPolicy=Adaptive",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx24g",
) else emptyList()))

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.plus(listOf(
  "-H:+NativeArchitecture",
))

val linuxOnlyArgs = defaultPlatformArgs.plus(listOf(
  "--static",
  "-march=compatibility",
  "-H:RuntimeCheckedCPUFeatures=AVX,AVX2",
  "-H:+StaticExecutableWithDynamicLibC",
)).plus(
  if (enableG1) listOf(
    "--gc=G1",
    "-H:+UseG1GC",
    "-XX:MaxRAMPercentage=40",
  ) else listOf(
    "--gc=serial",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  )
)

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
  release: Boolean = (!quickbuild && project.properties["elide.release"] != "true"),
): List<String> =
  commonNativeArgs.asSequence().plus(
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
    jvmDefs.map { "-D${it.key}=${it.value}" }
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" }
  ).plus(
    if (debug && !release) debugFlags else if (release) releaseFlags else emptyList()
  ).filterNotNull().toList()

graalvmNative {
  toolchainDetection = false
  testSupport = false

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
    all {
      resources.autodetect()
    }

    named("main") {
      imageName = "elide.debug"
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
      imageName = "elide-test"
      fallback = false
      quickBuild = quickbuild
      buildArgs.addAll(nativeCliImageArgs().plus(testOnlyArgs))
    }
  }
}

val decompressProfiles: TaskProvider<Copy> by tasks.registering(Copy::class) {
  from(zipTree("$projectDir/profiles.zip"))
  into("$buildDir/native/nativeOptimizedCompile")
}

/**
 * Build: CLI Docker Images
 */

tasks {
  shadowJar {
    exclude(
      "java-header-style.xml",
      "license.header",
    )
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguageBeta
    languageVersion = Elide.kotlinLanguageBeta
    jvmTarget = Elide.kotlinJvmTargetMaximum
    javaParameters = true
    freeCompilerArgs = ktCompilerArgs
    allWarningsAsErrors = false
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

configureJava9ModuleInfo(
  multiRelease = false,
)

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("net.java.dev.jna:jna"))
      .using(module("net.java.dev.jna:jna:${libs.versions.jna.get()}"))
  }
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
