@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import org.jetbrains.kotlin.konan.target.HostManager
import tools.elide.assets.EmbeddedScriptLanguage

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("com.github.johnrengelman.shadow")
  id("io.micronaut.application") version "4.2.0"
  id("io.micronaut.aot") version "4.2.1"
  id("dev.elide.build.site.backend")
  id("dev.elide.build.docker")
  id("dev.elide.buildtools.plugin")
  id("dev.elide.build.native.app")
  id("com.google.devtools.ksp")
  alias(libs.plugins.jib)
}

group = "dev.elide.site.docs"
version = rootProject.version as String

/**
 * Build: Site Native Image
 */

val commonNativeArgs = listOf(
  "--trace-object-instantiation=kotlin.reflect.jvm.internal.KTypeImpl",
)

val debugFlags = listOf(
  "-g",
  "-H:DashboardDump=elide-site",
  "-H:+DashboardAll",
)

val releaseFlags: List<String> = listOf(
  "-O2",
)

val jvmDefs = mapOf(
  "user.country" to "US",
  "user.language" to "en",
)

val hostedRuntimeOptions = mapOf(
  "IncludeLocales" to "en",
)

val initializeAtBuildTime: List<String> = emptyList()

val initializeAtRuntime: List<String> = emptyList()

val defaultPlatformArgs = listOf(
  "--libc=glibc",
)

val darwinOnlyArgs = defaultPlatformArgs

val linuxOnlyArgs = listOf(
  "--static",
  "--libc=musl",
  "-J-Xmx22G",
)

val testOnlyArgs: List<String> = emptyList()

val isEnterprise: Boolean = properties["elide.graalvm.variant"] == "ENTERPRISE"

val enterpriseOnlyFlags: List<String> = listOf(
  "--gc=G1",
  "--enable-sbom",
  "--pgo=${project.projectDir}/analysis/default.iprof",
  "-Dpolyglot.image-build-time.PreinitializeContexts=js",
)

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

fun nativeImageArgs(
  platform: String = "generic",
  target: String = "musl",
  debug: Boolean = quickbuild,
  release: Boolean = (!quickbuild && project.properties["elide.release"] == "true"),
  enterprise: Boolean = isEnterprise,
): List<String> =
  commonNativeArgs.asSequence().plus(
    initializeAtBuildTime.map { "--initialize-at-build-time=$it" }
  ).plus(
    initializeAtRuntime.map { "--initialize-at-run-time=$it" }
  ).plus(when (platform) {
    "darwin" -> darwinOnlyArgs
    else -> linuxOnlyArgs
  }).plus(
    jvmDefs.map { "-D${it.key}=${it.value}" }
  ).plus(
    hostedRuntimeOptions.map { "-H:${it.key}=${it.value}" }
  ).plus(
    if (debug) debugFlags else if (release) releaseFlags else emptyList()
  ).plus(
    if (enterprise) enterpriseOnlyFlags else emptyList()
  ).toList()

java {
  sourceCompatibility = JavaVersion.VERSION_19
  targetCompatibility = JavaVersion.VERSION_19
}

kotlin {
  target.compilations.all {
    kotlinOptions {
      jvmTarget = Elide.javaTargetMaximum
      javaParameters = true
      languageVersion = Elide.kotlinLanguage
      apiVersion = Elide.kotlinLanguage
      allWarningsAsErrors = true
      freeCompilerArgs = freeCompilerArgs.plus(Elide.jvmCompilerArgsBeta).toSortedSet().toList()
    }
  }
}

allOpen {
  annotations(listOf(
    "io.micronaut.aop.Around",
    "elide.server.annotations.Page",
  ))
}

elide {
  injectDependencies = false

  mode = if (devMode) {
    BuildMode.DEVELOPMENT
  } else {
    BuildMode.PRODUCTION
  }

  server {
    ssr(EmbeddedScriptLanguage.JS) {
      bundle(project(":site:docs:node"))
    }

    assets {
      bundler {
        format(tools.elide.assets.ManifestFormat.BINARY)
        compression {
          minimumSizeBytes(0)
          keepAllVariants()
        }
      }

      // stylesheet: `styles.base`
      stylesheet("styles.base") {
        sourceFile("${project(":site:docs:ui").projectDir}/src/main/assets/base.min.css")
      }

      // stylesheet: `styles.home`
      stylesheet("styles.home") {
        sourceFile("${project(":site:docs:ui").projectDir}/src/main/assets/home.min.css")
      }

      // script: `scripts.ui`
      script("scripts.ui") {
        from(project(":site:docs:ui"))
      }
    }
  }
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "$mainPackage.*",
    ))
  }
  aot {
    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    optimizeNetty.set(true)

    netty {
      enabled.set(true)
    }
  }
}

val mainPackage = "elide.site"
val mainEntry = "$mainPackage.DocsApp"
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"
val buildDocsSite: String by project.properties

application {
  mainClass.set(mainEntry)
  if (project.hasProperty("elide.vm.inspect") && project.properties["elide.vm.inspect"] == "true") {
    applicationDefaultJvmArgs = listOf(
      "-Delide.vm.inspect=true",
    )
  }
  if (gradle.startParameter.isContinuous ||
    (project.hasProperty("elide.mode") && project.properties["elide.mode"] == "dev")) {
    applicationDefaultJvmArgs = listOf(
      "-Dmicronaut.io.watch.restart=true",
      "-Dmicronaut.io.watch.enabled=true",
    )
  }
}

dependencies {
  annotationProcessor(mn.micronaut.serde.processor)
  compileOnly(libs.graalvm.svm)
  ksp(project(":tools:processor"))
  ksp(libs.autoService.ksp)
  api(project(":packages:base"))
  api(project(":packages:ssr"))
  api(project(":packages:server"))
  api(project(":packages:graalvm"))
  api(project(":packages:proto:proto-protobuf"))
  api(project(":site:docs:content"))

  implementation(libs.jsoup)
  implementation(libs.google.auto.service.annotations)
  implementation(libs.jackson.core)
  implementation(libs.jackson.databind)
  implementation(libs.jackson.jsr310)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.coroutines.reactor)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)
  implementation(libs.netty.tcnative)
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(libs.brotli)
  implementation(libs.netty.transport.native.unixCommon)

  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(mn.micronaut.cache.core)
  implementation(mn.micronaut.cache.caffeine)
  implementation(mn.micronaut.views.core)
  implementation(mn.micronaut.serde.api)
  implementation(mn.micronaut.jackson.databind)

  runtimeOnly(libs.logback)
  runtimeOnly(mn.snakeyaml)

  if (HostManager.hostIsMac) {
    implementation(libs.netty.resolver.dns.native.macos)
    implementation(libs.netty.transport.native.kqueue)
    implementation(libs.brotli.native.osx)
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-x86_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("osx-aarch_64") })
  } else if (HostManager.hostIsLinux) {
    implementation(libs.brotli.native.linux)
    implementation(libs.netty.transport.native.epoll)
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-aarch_64") })
  }

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(projects.packages.test)
  testImplementation(mn.micronaut.test.junit5)
}

val shadowAppJar: Configuration by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

tasks.shadowJar {
  mergeServiceFiles()
  archiveClassifier.set("shadow")
}

artifacts {
  add("shadowAppJar", tasks.shadowJar)
}

tasks.named("apiCheck").configure {
  onlyIf { false }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm19")
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("optimizedDockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm19")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native/alpine:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("optimizedDockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native/alpine:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/site/docs/native:opt-latest"
  ))
}

tasks {
  jib {
    from {
      image = "us-docker.pkg.dev/elide-fw/tools/runtime/jvm19:latest"
    }
    to {
      image = "us-docker.pkg.dev/elide-fw/samples/site/docs/jvm"
      tags = setOf("latest", "jib")
    }
    container {
      jvmFlags = listOf("-Delide.runtime=JVM", "-Xms512m", "-Xdebug")
      mainClass = mainEntry
      ports = listOf("8080", "50051")
      format = com.google.cloud.tools.jib.api.buildplan.ImageFormat.Docker
    }
  }
}

graalvmNative {
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
    enabled.set(System.getenv("GRAALVM_AGENT") == "true")

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
      fallback.set(false)
      quickBuild.set(true)
      buildArgs.addAll(nativeImageArgs())
    }

    named("optimized") {
      fallback.set(false)
      quickBuild.set(true)
      buildArgs.addAll(nativeImageArgs(release = true))
    }
  }
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.micronaut:micronaut-jackson-databind"))
            .using(module("io.micronaut.serde:micronaut-serde-jackson:${mn.versions.micronaut.serde.get()}"))
    }
}

val buildDocs by properties

tasks {
  if (buildDocs == "true") {
    named("dokkaJavadoc").configure {
      enabled = false
    }
  }
}

afterEvaluate {
  listOf(
    "shadowJar",
    "buildLayers",
    "optimizedJitJarAll",
    "optimizedBuildLayers",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}
