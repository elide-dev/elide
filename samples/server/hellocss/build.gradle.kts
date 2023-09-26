@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp")
  id("io.micronaut.docker")
  id("io.micronaut.application")
  id("io.micronaut.aot")
}

group = "dev.elide.samples"
version = rootProject.version as String

application {
  mainClass.set("hellocss.App")
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("hellocss.*")
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
  ksp(projects.tools.processor)
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
    "${project.properties["elide.publish.repo.docker.samples"]}/server/hellocss/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/hellocss/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/hellocss/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/hellocss/native:opt-latest"
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
    "optimizedBuildNativeLayersTask",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}
