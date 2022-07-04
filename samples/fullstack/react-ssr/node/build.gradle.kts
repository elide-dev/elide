@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask

plugins {
  idea
  distribution
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("dev.elide.buildtools.plugin")
  alias(libs.plugins.node)
  alias(libs.plugins.sonar)
}

group = "dev.elide.samples"
version = rootProject.version as String

val kotlinWrapperVersion = Versions.kotlinWrappers
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

elide {
  if (devMode) {
    mode.set(dev.elide.buildtools.gradle.plugin.BuildMode.DEVELOPMENT)
  } else {
    mode.set(dev.elide.buildtools.gradle.plugin.BuildMode.PRODUCTION)
  }
}

dependencies {
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm-js"))
  implementation(project(":packages:graalvm-react"))
  implementation(project(":samples:fullstack:react-ssr:frontend"))

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val rootPackageJson by rootProject.tasks.getting(RootPackageJsonTask::class)

node {
  download.set(false)
  nodeProjectDir.set(rootPackageJson.rootPackageJson.parentFile.normalize())
}
