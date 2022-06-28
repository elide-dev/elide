@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  idea
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.sonar)
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
  implementation(project(":packages:base"))
  implementation(kotlin("stdlib-js"))
  implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:0.0.1-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react:${Versions.react}-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:${Versions.react}-${Versions.kotlinWrappers}")
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
