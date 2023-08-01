@file:Suppress(
  "DSL_SCOPE_VIOLATION",
  "unused",
  "UNUSED_VARIABLE",
)

plugins {
  java
  `jvm-test-suite`
  distribution
  publishing
  jacoco

  id("com.github.gmazzo.buildconfig")
  id("com.github.johnrengelman.shadow")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.aot")
  id("dev.elide.build.jvm")
  id("org.jetbrains.dokka")
}

group = "dev.elide.tools"
version = rootProject.version as String

val entrypoint = "elide.tool.bundler.Bundler"
val javaVersion = "11"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

buildConfig {
  className("ElideBundlerTool")
  packageName("elide.tool.bundler.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"${libs.versions.elide.asProvider().get()}\"")
}

ktlint {
  debug = false
  verbose = false
  android = false
  outputToConsole = false
  ignoreFailures = true
  enableExperimentalRules = true
}

// JVM: Testing
// ------------
// JVM test suite configuration.
testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

val extraArgs: List<String> = emptyList()

kotlin {
  target.compilations.all {
    kotlinOptions {
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      jvmTarget = javaVersion
      javaParameters = true
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(extraArgs)
      allWarningsAsErrors = true
    }
  }

  afterEvaluate {
    target.compilations.all {
      kotlinOptions {
        jvmTarget = Elide.javaTargetMinimum
        languageVersion = Elide.kotlinLanguage
        apiVersion = Elide.kotlinLanguage
        javaParameters = true
        freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(extraArgs)
        allWarningsAsErrors = true
      }
    }
  }
}

dependencies {
  api(libs.slf4j)

  annotationProcessor(libs.micronaut.inject.java)
  annotationProcessor(libs.picocli.codegen)

  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))

  implementation(libs.picocli)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)

  implementation(libs.micronaut.context)
  implementation(libs.micronaut.picocli)
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.kotlin.extension.functions)
  implementation(libs.micronaut.kotlin.runtime)

  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-flatbuffers"))

  implementation(libs.lz4)
  implementation(libs.sqlite)
  implementation(libs.jimfs)
  implementation(libs.larray.core)
  implementation(libs.larray.buffer)
  implementation(libs.larray.mmap)
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
  runtime = io.micronaut.gradle.MicronautRuntime.NONE
  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.tool.bundler",
      "elide.tool.bundler.*",
    ))
  }

  aot {
    configFile = file("$projectDir/aot-native.properties")

    optimizeServiceLoading = true
    convertYamlToJava = true
    precomputeOperations = true
    cacheEnvironment = true
    optimizeClassLoading = true
  }
}

tasks {
  compileKotlin {
    kotlinOptions {
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      jvmTarget = javaVersion
      javaParameters = true
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(extraArgs)
      allWarningsAsErrors = true
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
  standardInput = System.`in`
  standardOutput = System.out
}

afterEvaluate {
  tasks.named<JavaExec>("optimizedRun") {
    systemProperty("micronaut.environments", "dev")
    systemProperty("picocli.ansi", "tty")
  }
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
 * Build: Bundler Native Image
 */

val commonNativeArgs = listOf(
  "--gc=serial",
  "--no-fallback",
  "--install-exit-handlers",
  "-H:DashboardDump=elide-bundler",
  "-H:+DashboardAll",
)

val debugFlags = listOf(
  "-g",
)

val releaseFlags = listOf(
  "-O1",
)

val jvmDefs = mapOf(
  "user.country" to "US",
  "user.language" to "en",
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTime = listOf(
  "org.slf4j.LoggerFactory",
  "org.slf4j.simple.SimpleLogger",
  "org.slf4j.impl.StaticLoggerBinder",
  "com.google.common.jimfs.SystemJimfsFileSystemProvider",
)

val initializeAtRuntime: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val darwinOnlyArgs = defaultPlatformArgs

val linuxOnlyArgs = listOf(
  "--static",
  "--libc=glibc",
)

val muslArgs = listOf(
  "--libc=musl",
)

val testOnlyArgs: List<String> = emptyList()

val isEnterprise: Boolean = properties["elide.graalvm.variant"] == "ENTERPRISE"
val enterpriseOnlyFlags: List<String> = listOf(
  "--enable-sbom",
  "-H:+AOTInliner",
)

fun nativeImageArgs(
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
  ).plus(
    if (enterprise) enterpriseOnlyFlags else emptyList()
  ).toList()

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
      imageName = "bundler"
      fallback = false
      buildArgs.addAll(nativeImageArgs())
      quickBuild = quickbuild
      sharedLibrary = false
      systemProperty("picocli.ansi", "tty")

      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }

    named("optimized") {
      imageName = "bundler"
      fallback = false
      buildArgs.addAll(nativeImageArgs())
      quickBuild = quickbuild
      sharedLibrary = false
      systemProperty("picocli.ansi", "tty")

      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }

    named("test") {
      imageName = "bundler-test"
      fallback = false
      buildArgs.addAll(nativeImageArgs().plus(testOnlyArgs))
      quickBuild = quickbuild

      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }
  }
}


/**
 * Build: Bundler Docker Images
 */

tasks {
  dockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm20:latest"
    buildStrategy = io.micronaut.gradle.docker.DockerBuildStrategy.DEFAULT
  }

  optimizedDockerfileNative {
    graalImage = "${project.properties["elide.publish.repo.docker.tools"]}/gvm20:latest"
    buildStrategy = io.micronaut.gradle.docker.DockerBuildStrategy.DEFAULT
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguageBeta
    languageVersion = Elide.kotlinLanguageBeta
    jvmTarget = Elide.javaTargetMinimum
    javaParameters = true
    freeCompilerArgs = Elide.jvmCompilerArgs
    allWarningsAsErrors = true
    incremental = true
  }
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images = listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/bundler/native:latest"
  )
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images = listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/bundler/native:opt-latest"
  )
}

// Bundler tool is native-only.

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  enabled = false
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  enabled = false
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("net.java.dev.jna:jna"))
      .using(module("net.java.dev.jna:jna:${libs.versions.jna.get()}"))
  }
}

afterEvaluate {
  listOf(
    "buildLayers",
    "optimizedBuildLayers",
    "shadowJar",
    "optimizedJitJarAll",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}

