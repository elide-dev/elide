@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import elide.dev.buildtools.gradle.tasks.GenerateEsBuildConfig
import elide.dev.buildtools.gradle.tasks.GenerateEsBuildConfig.Mode
import elide.dev.buildtools.gradle.tasks.GenerateEsBuildConfig.Mode.*
import elide.dev.buildtools.gradle.tasks.outputBundleFile
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import java.util.*

plugins {
  idea
  distribution
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.sonar)
}

group = "dev.elide.samples"
version = rootProject.version as String

val devMode = (project.property("elide.buildMode") ?: "dev") == "dev"

kotlin {
  js(IR) {
    nodejs {
      binaries.executable()
    }
  }
}

dependencies {
  implementation(project(":packages:base"))
  implementation(project(":packages:graalvm-js"))
  implementation(npm("esbuild", libs.versions.npm.esbuild.get()))
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
