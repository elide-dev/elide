import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  `java-platform`
  distribution
  signing
  idea

  id("dev.elide.build.core")
}

group = "dev.elide.tools"
version = rootProject.version as String

dependencies {
  constraints {
    // Kotlin.
    api(kotlin("stdlib"))

    api(libs.elide.tools.compilerUtil)
    api(libs.elide.kotlin.plugin.injekt)
    api(libs.elide.kotlin.plugin.interakt)
    api(libs.elide.kotlin.plugin.redakt)
    api(libs.elide.kotlin.plugin.sekret)
  }
}

sonarqube {
  isSkipProject = true
}

publishing {
  elideTarget(
    project,
    label = "Elide Tools: Substrate BOM",
    group = project.group as String,
    artifact = "elide-substrate-bom",
    summary = "BOM for Kotlin compiler plugins and other core project infrastructure.",
    bom = true,
  )
}
