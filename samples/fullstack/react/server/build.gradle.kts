@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import io.micronaut.gradle.MicronautRuntime.NETTY

plugins {
  kotlin("jvm")
  id(libs.plugins.ksp.get().pluginId)
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("io.micronaut.graalvm")
  id("io.micronaut.docker")
  id("dev.elide.buildtools.plugin")
}

group = "dev.elide.samples"
version = rootProject.version as String

elide {
  injectDependencies = false

  server {
    assets {
      script("scripts.ui") {
        from(projects.samples.fullstack.react.frontend)
      }
    }
  }
}

application {
  mainClass = "fullstack.react.App"
}

micronaut {
  version = libs.versions.micronaut.lib.get()
  runtime = NETTY

  processing {
    incremental = true
    annotations.add("fullstack.react.*")
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

dependencies {
  implementation(projects.packages.server)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.wrappers.css)
  runtimeOnly(libs.logback)
  runtimeOnly(mn.snakeyaml)
}

tasks.withType<Copy>().named("processResources") {
  dependsOn("copyStatic")
}

tasks.register<Copy>("copyStatic") {
  from("src/main/resources/static/**/*.*")
  into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/native:opt-latest"
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
