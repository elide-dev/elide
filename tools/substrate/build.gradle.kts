@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  distribution
  signing
  idea

  alias(libs.plugins.ktlint)
  alias(libs.plugins.dokka)
  alias(libs.plugins.versionCheck)
  alias(libs.plugins.testLogger)
  id(libs.plugins.detekt.get().pluginId)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
}

val allPlugins = listOf(
  "injekt",
  "interakt",
  "redakt",
  "sekret",
)

group = "dev.elide.tools"
version = rootProject.version as String

tasks.create("buildPlugins") {
  description = "Build all Kotlin compiler plugins"
  dependsOn(allPlugins.map { ":$it:build" })
}

val libPlugins = libs.plugins

tasks.named("build").configure {
  dependsOn("buildPlugins")
}

tasks.named("publish").configure {
  dependsOn(allPlugins.map {
    ":$it:publish"
  })
}

publishing {
  elideTarget(
    project,
    label = "Elide Tools: Substrate",
    group = project.group as String,
    artifact = "elide-substrate",
    summary = "Kotlin compiler plugins and other core project infrastructure.",
    parent = true,
  )
}
