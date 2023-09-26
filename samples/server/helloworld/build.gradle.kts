@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow")
//  id("dev.elide.build.samples.backend")
//  id("dev.elide.build.docker")
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("com.google.devtools.ksp")
}

group = "dev.elide.samples"
version = rootProject.version as String

application {
  mainClass.set("helloworld.App")
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("helloworld.*")
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

configurations.all {
  resolutionStrategy {
    preferProjectModules()
  }
}

dependencies {
  ksp(projects.tools.processor)
  implementation(projects.packages.server)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.runtime)
  implementation(libs.kotlinx.html.jvm)
  runtimeOnly(libs.logback)
}

val shadowAppJar by configurations.creating {
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

tasks.withType<Copy>().named("processResources") {
  dependsOn("copyStatic")
}

tasks.register<Copy>("copyStatic") {
  from("src/main/resources/static/**/*.*")
  into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/helloworld/jvm:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/helloworld/jvm:opt-latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/helloworld/native:latest"
  ))
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("optimizedDockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/server/helloworld/native:opt-latest"
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
    "shadowJar",
    "buildLayers",
    "optimizedBuildLayers",
    "optimizedJitJarAll",
  ).forEach {
    tasks.named(it).configure {
      doNotTrackState("too big for build cache")
    }
  }
}

