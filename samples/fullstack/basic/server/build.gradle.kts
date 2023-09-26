@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  kotlin("jvm")
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("io.micronaut.docker")
  id("io.micronaut.graalvm")
  id("dev.elide.buildtools.plugin")
}

group = "dev.elide.samples"
version = rootProject.version as String

elide {
  injectDependencies = false

  server {
    assets {
      script("scripts.ui") {
        from(project(":samples:fullstack:basic:frontend"))
      }
    }
  }
}

application {
  mainClass.set("fullstack.basic.App")
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("fullstack.basic.*")
  }
  aot {
    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    netty {
      enabled.set(true)
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
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/basic/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/basic/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/basic/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/basic/native:opt-latest"
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
