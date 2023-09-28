@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode.DEVELOPMENT
import dev.elide.buildtools.gradle.plugin.BuildMode.PRODUCTION
import io.micronaut.gradle.MicronautRuntime.NETTY

plugins {
  kotlin("jvm")
  id(libs.plugins.ksp.get().pluginId)
  id("dev.elide.buildtools.plugin")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.docker")
  id("io.micronaut.aot")
}

group = "dev.elide.samples"
version = rootProject.version as String
val devMode = (project.property("elide.buildMode") ?: "dev") != "prod"

kotlin {
  // nothing at this time
}

application {
  mainClass.set("fullstack.ssr.App")
}

elide {
  // we manage deps internally for this sample (it's embedded within the elide codebase)
  injectDependencies = false

  // swap build mode based on `devMode` flag
  mode = if (devMode) {
    DEVELOPMENT
  } else {
    PRODUCTION
  }

  server {
    ssr(tools.elide.assets.EmbeddedScriptLanguage.JS) {
      bundle(projects.samples.fullstack.ssr.node)
    }
  }
}

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = NETTY

  processing {
    incremental = true
    annotations.add("fullstack.ssr.*")
  }
  aot {
    optimizeServiceLoading = true
    convertYamlToJava = true
    precomputeOperations = true
    cacheEnvironment = true

    netty {
      enabled = true
    }
  }
}

graalvmNative {
  testSupport = false

  metadataRepository {
    enabled = true
    version = "0.3.3"
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
      fallback = false
      quickBuild = true
      buildArgs.addAll(listOf(
        "--no-fallback",
        "--language:js",
//        "--language:regex",
//        "--enable-http",
//        "--enable-https",
//        "--gc=G1",
//        "--static",
//        "--libc=glibc",
//        "--enable-all-security-services",
//        "--install-exit-handlers",
//        "--report-unsupported-elements-at-runtime",
//        "-Duser.country=US",
//        "-Duser.language=en",
//        "-H:IncludeLocales=en",
//        "-H:+InstallExitHandlers",
//        "-H:+ReportExceptionStackTraces",
//        "--pgo-instrument",
//        "-dsa",
//        "--language:js",
//        "--language:regex",
        "--enable-all-security-services",
//        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))
    }
  }
}

dependencies {
  implementation(projects.packages.base)
  implementation(projects.packages.server)
  implementation(projects.packages.graalvm)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.wrappers.css)
  runtimeOnly(libs.logback)
  runtimeOnly(mn.snakeyaml)
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/ssr/native:opt-latest"
  ))
}

tasks {
  distTar {
    enabled = false
  }
  distZip {
    enabled = false
  }
}

afterEvaluate {
  listOf(
    "buildLayers",
    "optimizedBuildLayers",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}
