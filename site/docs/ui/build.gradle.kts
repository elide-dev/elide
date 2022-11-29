@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.site.frontend")
}

group = "dev.elide.site.docs"
version = rootProject.version as String

val kotlinWrapperVersion = libs.versions.kotlinxWrappers.get()
val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

kotlin {
  js(IR) {
    binaries.executable()
    browser {
      commonWebpackConfig {
        sourceMaps = false
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
  implementation(project(":packages:frontend"))
  implementation(project(":packages:graalvm-react"))

  implementation(kotlin("stdlib-js"))
  implementation(libs.kotlinx.wrappers.browser)
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
}

val assetDist by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

artifacts {
  add(assetDist.name, tasks.named("browserDistribution").map { it.outputs.files.files.single() })
}
