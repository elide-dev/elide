@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  java
  jacoco
  idea
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.micronautApplication)
  alias(libs.plugins.micronautAot)
  alias(libs.plugins.sonar)
}

group = "dev.elide.samples"
version = rootProject.version as String

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

application {
  mainClass.set("fullstack.react.App")
}

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("fullstack.react.*")
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

val browserDist: Configuration by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
}

dependencies {
  implementation(project(":packages:server"))
  implementation("io.micronaut:micronaut-context")
  implementation("io.micronaut:micronaut-runtime")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-${Versions.kotlinWrappers}")
  runtimeOnly("ch.qos.logback:logback-classic:${Versions.logbackClassic}")

  browserDist(
    project(
      mapOf(
        "path" to ":samples:fullstack:react:frontend",
        "configuration" to "browserDist"
      )
    )
  )
}

tasks.withType<Copy>().named("processResources") {
  dependsOn("copyJs")
  dependsOn("copyStatic")
}

tasks.register<Copy>("copyJs") {
  from(browserDist)
  into("$buildDir/resources/main/assets/js")
}

tasks.register<Copy>("copyStatic") {
  from("src/main/resources/static/**/*.*")
  into("$buildDir/resources/main/static")
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/base:latest")
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/jvm:latest"
  ))
  this.target
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuildNative") {
  images.set(listOf(
    "${project.properties["elide.publish.repo.docker.samples"]}/fullstack/react/native:latest"
  ))
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage(project.properties["elide.samples.docker.base.native"] as String)
  args("-H:+StaticExecutableWithDynamicLibC")
}
