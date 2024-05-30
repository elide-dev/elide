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
  kotlin("kapt")
  id("dev.elide.buildtools.plugin")
  id("io.micronaut.application")
  id("io.micronaut.graalvm")
  id("io.micronaut.docker")
  id("io.micronaut.aot")
}

group = "dev.elide.samples"
version = rootProject.version as String
val devMode = (project.property("elide.buildMode") ?: "dev") != "prod"

application {
  mainClass.set("fullstack.ssr.App")
}

elideApp {
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
      bundle(projects.fullstack.ssr.node)
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_22
  targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
  jvmToolchain(22)
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
        "--enable-all-security-services",
      ))
    }
  }
}

dependencies {
  kapt(mn.micronaut.inject.java)
  implementation(framework.elide.base)
  implementation(framework.elide.server)
  implementation(framework.elide.graalvm)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.reactor)
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
