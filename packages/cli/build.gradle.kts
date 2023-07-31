@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import Java9Modularity.configure as configureJava9ModuleInfo
import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.docker.DockerBuildStrategy
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

val enableEspresso = true
val enableWasm = true
val enableLlvm = false
val enablePython = false
val enableRuby = false
val enableTools = true
val enableSbom = true
val enableG1 = true
val enablePgo = true
val enablePgoInstrumentation = false
val enableMosaic = true
val enableProguard = false
val enableUpx = false
val enableDashboard = false

val ktCompilerArgs = listOf(
  "-progressive",
  "-Xallow-unstable-dependencies",
  "-Xcontext-receivers",
  "-Xemit-jvm-type-annotations",
  "-Xlambdas=indy",
  "-Xsam-conversions=indy",
  "-Xjsr305=strict",
  "-Xjvm-default=all",
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

// use consistent compose plugin version
//the<com.jakewharton.mosaic.gradle.MosaicExtension>().kotlinCompilerPlugin =
//  libs.versions.compose.get()

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

  kapt(libs.micronaut.inject.java)
  kapt(libs.picocli.codegen)

  api(project(":packages:base"))
  implementation(project(":packages:graalvm"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.kotlin.scripting.common)
  implementation(libs.kotlin.scripting.dependencies)
  implementation(libs.kotlin.scripting.dependencies.maven)
  implementation(libs.kotlin.scripting.jvm)
  implementation(libs.kotlin.scripting.jvm.host)
  implementation(libs.kotlin.scripting.jvm.engine)
  implementation(libs.logback)

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

  implementation(project(":packages:proto:proto-protobuf"))
  runtimeOnly(project(":packages:proto:proto-kotlinx"))

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
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  "-H:+BuildReport",
  "-H:CStandard=C11",
  "-H:DefaultCharset=UTF-8",
  "-H:+UseContainerSupport",
  "-H:+UseCompressedReferences",
  "-H:+AllowJRTFileSystem",
  "-H:+ReportExceptionStackTraces",
  "-H:-EnableAllSecurityServices",
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
  if (enableTools) listOf(
    "--tool:chromeinspector",
    "--tool:coverage",
    "--tool:lsp",
    "--tool:sandbox",
    "--tool:dap",
    "--tool:insight",
    "--tool:insightheap",
    "--tool:profiler",
  ) else emptyList()
).plus(
  jvmModuleArgs
)

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
)

// CFlags for release mode.
val releaseCFlags: List<String> = listOf(
  "-O3",
  "-flto",
)

// PGO profiles to specify in release mode.
val profiles: List<String> = listOf(
  "cli.iprof",
  "serve.iprof",
)

// Full release flags (for all operating systems and platforms).
val releaseFlags: List<String> = listOf(
  "-O2",
  "-dsa",
  "-H:+AOTInliner",
  "-H:+BuildReport",
  "-H:+MLProfileInference",
  "-H:+LocalizationOptimizedMode",
  "-H:+BouncyCastleIntrinsics",
  "-H:+VectorPolynomialIntrinsics",
  "-H:+VectorizeSIMD",
  "-H:+AOTAggregateProfiles",
  "-H:+AggressiveColdCodeOptimizations",
  "-H:+LSRAOptimization",
  "-H:+RemoveUnusedSymbols",
  "-R:+BouncyCastleIntrinsics",
  "-J-Djdk.image.use.jvm.map=false",
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
  "com.sun.tools.doclint",
  "jdk.jshell.Snippet${'$'}SubKind",
  "com.sun.tools.javac.parser.Tokens${'$'}TokenKind",
)

val initializeAtRuntime: List<String> = listOf(
  "ch.qos.logback.core.AsyncAppenderBase${'$'}Worker",
  "io.micronaut.core.util.KotlinUtils",
  "io.micrometer.common.util.internal.logging.Slf4JLoggerFactory",
  "com.sun.tools.javac.file.Locations",
)

val rerunAtRuntime: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val windowsOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:-AuxiliaryEngineCache",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList()))

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
  "--gc=serial",
  "-Delide.vm.engine.preinitialize=true",
  "-H:+AuxiliaryEngineCache",
  "-H:InitialCollectionPolicy=Adaptive",
  "-R:MaximumHeapSizePercent=80",
).plus(if (project.properties["elide.ci"] == "true") listOf(
  "-J-Xmx12g",
) else emptyList()))

val windowsReleaseArgs = windowsOnlyArgs

val darwinReleaseArgs = darwinOnlyArgs.plus(listOf(
  "-H:+NativeArchitecture",
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
    "-H:+AuxiliaryEngineCache",
    "-Delide.vm.engine.preinitialize=true",
    "-R:MaximumHeapSizePercent=80",
    "-H:InitialCollectionPolicy=Adaptive",
  )
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
    // all {
    //   resources.autodetect()
    // }

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
  from(zipTree(layout.projectDirectory.file("profiles.zip")))
  into(layout.buildDirectory.dir("native/nativeOptimizedCompile"))
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

tasks.withType<KotlinCompile>().configureEach {
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

configureJava9ModuleInfo(project)

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

// Unused dependencies:
// implementation(libs.picocli.jline3)
