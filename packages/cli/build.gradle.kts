@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import Java9Modularity.configureJava9ModuleInfo
import io.micronaut.gradle.MicronautRuntime
import io.micronaut.gradle.docker.DockerBuildStrategy
import org.jetbrains.kotlin.gradle.internal.KaptTask
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
}

group = "dev.elide"
version = rootProject.version as String

val entrypoint = "elide.tool.cli.ElideTool"

val enableEspresso = false
val enableWasm = true
val enableLlvm = false
val enablePython = false
val enableRuby = false
val enablePgo = true
val enablePgoInstrumentation = false
val enableSbom = true

java {
  sourceCompatibility = JavaVersion.VERSION_19
  targetCompatibility = JavaVersion.VERSION_19
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
  explicitApi()

  target.compilations.all {
    kotlinOptions {
      jvmTarget = Elide.kotlinJvmTargetMaximum
      javaParameters = true
      languageVersion = Elide.kotlinLanguage
      apiVersion = Elide.kotlinLanguage
      allWarningsAsErrors = true
      freeCompilerArgs = Elide.jvmCompilerArgsBeta
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
  kapt(libs.micronaut.validation)
  kapt(libs.picocli.codegen)

  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm"))
  implementation(project(":tools:bundler"))
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlin.scripting.common)
  implementation(libs.kotlin.scripting.jvm)
  implementation(libs.kotlin.scripting.jvm.host)

  implementation(libs.picocli)
  implementation(libs.picocli.jansi.graalvm)
  implementation(libs.picocli.jline3)
  implementation(libs.kotter)
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

  compileOnly(libs.graalvm.sdk)
  implementation(libs.logback)

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

    optimizeServiceLoading = true
    convertYamlToJava = false
    precomputeOperations = true
    cacheEnvironment = true
    optimizeClassLoading = true

    netty {
      enabled = false
      machineId = "elide"
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
  jvmArgs(
    "--add-opens=java.base/java.io=ALL-UNNAMED",
  )
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
  "--gc=serial",
  "--no-fallback",
  "--enable-preview",
  "--enable-http",
  "--enable-https",
  "--install-exit-handlers",
  "-H:DashboardDump=elide-tool",
  "-H:+DashboardAll",
  "-H:+AuxiliaryEngineCache",
  "-Dpolyglot.image-build-time.PreinitializeContexts=js",
).plus(listOfNotNull(
  if (enableEspresso) "--language:java" else null,
  if (enableWasm) "--language:wasm" else null,
  if (enableLlvm) "--language:llvm" else null,
  if (enablePython) "--language:python" else null,
  if (enableRuby) "--language:ruby" else null,
))

val debugFlags = listOfNotNull(
  "-g",
  "-march=compatibility",
  if (enablePgoInstrumentation) "--pgo-instrument" else null,
)

val releaseFlags = listOf(
  "-O2",
  "-H:+AOTInliner",
  if (enablePgo) "--pgo=cli.iprof" else null,
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
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
  "org.slf4j.LoggerFactory",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",
)

val initializeAtRuntime: List<String> = emptyList()

val rerunAtRuntime: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val darwinOnlyArgs = defaultPlatformArgs.plus(listOf(
  "-march=native",
))

val linuxOnlyArgs = listOf(
  "--static",
  "--libc=glibc",
  "-march=compatibility",
)

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
  enterprise: Boolean = isEnterprise,
): List<String> =
  commonNativeArgs.asSequence().plus(
    initializeAtBuildTime.map { "--initialize-at-build-time=$it" }
  ).plus(
    initializeAtRuntime.map { "--initialize-at-run-time=$it" }
  ).plus(
    rerunAtRuntime.map { "--rerun-class-initialization-at-runtime=$it" }
  ).plus(when (platform) {
    "darwin" -> darwinOnlyArgs
    "linux" -> if (target == "musl") muslArgs else linuxOnlyArgs
    else -> defaultPlatformArgs
  }).plus(
    jvmDefs.map { "-D${it.key}=${it.value}" }
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" }
  ).plus(
    if (debug) debugFlags else if (release) releaseFlags else emptyList()
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
      imageName = "elide"
      fallback = false
      buildArgs.addAll(nativeCliImageArgs(debug = quickbuild, release = !quickbuild))
      quickBuild = quickbuild
      sharedLibrary = false
      systemProperty("picocli.ansi", "tty")
    }

    named("optimized") {
      imageName = "elide"
      fallback = false
      buildArgs.addAll(nativeCliImageArgs(debug = false, release = true))
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

  dockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm19:latest"
    buildStrategy = DockerBuildStrategy.DEFAULT
  }

  optimizedDockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm19:latest"
    buildStrategy = DockerBuildStrategy.DEFAULT
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguageBeta
    languageVersion = Elide.kotlinLanguageBeta
    jvmTarget = Elide.kotlinJvmTargetMaximum
    javaParameters = true
    freeCompilerArgs = Elide.jvmCompilerArgs
    allWarningsAsErrors = true
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

  tasks.withType(KaptTask::class.java).configureEach {

  }

  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    kotlinOptions {
      apiVersion = Elide.kotlinLanguageBeta
      languageVersion = Elide.kotlinLanguageBeta
      jvmTarget = Elide.kotlinJvmTargetMaximum
      javaParameters = true
      freeCompilerArgs = Elide.jvmCompilerArgs
      allWarningsAsErrors = true
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

