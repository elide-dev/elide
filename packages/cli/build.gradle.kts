@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

import proguard.gradle.ProGuardTask
import Java9Modularity.configureJava9ModuleInfo

plugins {
  `java-library`
  distribution
  publishing
  jacoco

  kotlin("plugin.serialization")
  id("com.github.gmazzo.buildconfig")
  id("com.github.johnrengelman.shadow")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.aot")
  id("dev.elide.build.docker")
  id("dev.elide.build.jvm.kapt")
  id("dev.elide.build.native.app")
}

group = "dev.elide"
version = rootProject.version as String

val buildFat = false
val nativeArch = "amd64"
val entrypoint = "elide.tool.cli.ElideTool"

java {
  sourceCompatibility = JavaVersion.VERSION_19
  targetCompatibility = JavaVersion.VERSION_16
}

kotlin {
  explicitApi()
}

buildConfig {
  className("ElideCLITool")
  packageName("elide.tool.cli.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"${libs.versions.elide.asProvider().get()}\"")
}

dependencies {
  api(libs.slf4j)

  kapt(libs.micronaut.inject.java)
  kapt(libs.micronaut.validation)
  kapt(libs.picocli.codegen)
  kapt(libs.micronaut.serde.processor)

  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm"))
  implementation(project(":packages:model"))
  implementation(project(":packages:proto"))
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))

  implementation(libs.picocli)
  implementation(libs.bouncycastle)
  implementation(libs.picocli.jansi.graalvm)

  implementation(libs.kotlin.compiler.embedded)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.picocli)
  implementation(libs.micronaut.http.client)
  implementation(libs.micronaut.graal)
  implementation(libs.micronaut.kotlin.extension.functions)
  implementation(libs.micronaut.kotlin.runtime)

  compileOnly(libs.graalvm.sdk)
  compileOnly(libs.graalvm.js.engine)
  compileOnly(libs.graalvm.espresso.polyglot)
  compileOnly(libs.graalvm.espresso.hotswap)

  implementation(libs.logback)

  implementation(
    "io.netty:netty-resolver-dns-native-macos:4.1.81.Final:osx-aarch_64"
  )
  implementation(
    "io.netty:netty-resolver-dns-native-macos:4.1.81.Final:osx-x86_64"
  )

  runtimeOnly(libs.micronaut.runtime)
  runtimeOnly(libs.micronaut.runtime.osx)

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
  mainClass.set(entrypoint)
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

tasks.create("proguardShadow", ProGuardTask::class.java)
tasks.create("proguardOptimized", ProGuardTask::class.java)
val proguardRules = file("$projectDir/rules.pro")

tasks.named("proguardShadow", ProGuardTask::class.java).configure {
  configuration(proguardRules)
  injars(file("$buildDir/libs/cli-all.jar"))
  outjars(file("$buildDir/libs/cli-all-min.jar"))
  dependsOn(tasks.named("shadowJar"))
//  libraryjars(configurations.named("runtimeClasspath").get())
}

tasks.named("proguardOptimized", ProGuardTask::class.java).configure {
  configuration(proguardRules)
  injars(file("$buildDir/libs/cli-optimized.jar"))
  outjars(file("$buildDir/libs/cli-optimized-min.jar"))
  dependsOn("optimizedJitJarAll")
//  libraryjars(configurations.named("runtimeClasspath").get())
}

tasks.create("proguard") {
  dependsOn("proguardShadow", "proguardOptimized")
}

tasks.jar { enabled = false }
artifacts.archives(tasks.shadowJar)


/**
 * Framework: Micronaut
 */

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NONE)
  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "elide.tool.cli.*",
    ))
  }

  aot {
    configFile.set(file("$projectDir/aot-native.properties"))

    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
  }
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("io.micronaut:micronaut-jackson-databind"))
      .using(module("io.micronaut.serde:micronaut-serde-jackson:${libs.versions.micronaut.serde.get()}"))
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
 * Build: CLI Native Image
 */

val commonNativeArgs = listOf(
  "--language:js",
  "--gc=serial",
  "--no-fallback",
  "--enable-http",
  "--enable-https",
  "--enable-all-security-services",
  "--install-exit-handlers",
  "-H:DashboardDump=elide-tool",
  "-H:+DashboardAll",
  "-H:-SpawnIsolates",
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
  toolchainDetection.set(false)
  testSupport.set(false)

  metadataRepository {
    enabled.set(true)
    version.set(GraalVMVersions.graalvmMetadata)
  }

  agent {
    defaultMode.set("standard")
    builtinCallerFilter.set(true)
    builtinHeuristicFilter.set(true)
    enableExperimentalPredefinedClasses.set(false)
    enableExperimentalUnsafeAllocationTracing.set(false)
    trackReflectionMetadata.set(true)
    enabled.set(true)

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting.set(true)
    }
  }

  binaries {
    named("main") {
      imageName.set("elide")
      fallback.set(false)
      buildArgs.addAll(nativeCliImageArgs())
      quickBuild.set(quickbuild)
      sharedLibrary.set(false)
      systemProperty("picocli.ansi", "tty")

      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            }))
          }
        }
      })
    }

    named("optimized") {
      imageName.set("elide")
      fallback.set(false)
      buildArgs.addAll(nativeCliImageArgs())
      quickBuild.set(quickbuild)
      sharedLibrary.set(false)
      systemProperty("picocli.ansi", "tty")

      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            }))
          }
        }
      })
    }

    named("test") {
      imageName.set("elide-test")
      fallback.set(false)
      buildArgs.addAll(nativeCliImageArgs().plus(testOnlyArgs))
      quickBuild.set(quickbuild)

      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            }))
          }
        }
      })
    }
  }
}


/**
 * Build from shadow JARs
 */

if (buildFat) {
  tasks.named<Jar>("jar") {
    archiveClassifier.set("default")
  }

  tasks.named<Jar>("shadowJar") {
    val stubbed: String? = null
    setProperty("zip64", true)
    archiveClassifier.set(stubbed)
  }

  afterEvaluate {
    val optimizedShadowJar = tasks.named<Jar>("optimizedJitJarAll")
    optimizedShadowJar.configure {
      setProperty("zip64", true)
      archiveClassifier.set("optimized")
    }

    if (!quickbuild) {
      tasks.named<org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask>("nativeCompile") {
        classpathJar.set(optimizedShadowJar.flatMap { it.archiveFile })
      }
    }
  }
} else {
  afterEvaluate {
    val optimizedShadowJar = tasks.named<Jar>("optimizedJitJarAll")
    optimizedShadowJar.configure {
      setProperty("zip64", true)
      archiveClassifier.set("optimized")
    }
  }
}


/**
 * Build: CLI Docker Images
 */

tasks {
  dockerfileNative {
    graalArch.set(nativeArch)
    graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/gvm19:latest")
    buildStrategy.set(io.micronaut.gradle.docker.DockerBuildStrategy.DEFAULT)
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguageBeta
    languageVersion = Elide.kotlinLanguageBeta
    jvmTarget = Elide.javaTargetProguard
    javaParameters = true
    freeCompilerArgs = Elide.jvmCompilerArgs
    allWarningsAsErrors = true
    incremental = true
  }
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/elide/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.tools"]}/cli/elide/native:opt-latest"
  ))
}

// CLI tool is native-only.

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  enabled = false
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  enabled = false
}

configureJava9ModuleInfo(
  multiRelease = true,
)
