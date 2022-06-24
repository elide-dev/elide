@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

plugins {
  idea
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("org.sonarqube")
}

group = "dev.elide.samples"
version = rootProject.version as String

val kotlinWrapperVersion = Versions.kotlinWrappers
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

kotlin {
  js(IR) {
    binaries.executable()
    browser {
      commonWebpackConfig {
        sourceMaps = devMode
        cssSupport.enabled = true
        mode = if (devMode) {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
        } else {
          org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION
        }
      }
    }
  }
}

dependencies {
  implementation(project(":base"))
  implementation(kotlin("stdlib-js"))
  implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:0.0.1-${kotlinWrapperVersion}")
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val browserDist by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

artifacts {
  add(browserDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}
