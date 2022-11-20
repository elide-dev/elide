import GradleProject.projectConstants
import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  distribution
  signing

  id("dev.elide.build")
  id("dev.elide.build.kotlin.compilerPlugin")
}

projectConstants(
  packageName = "elide.tools.kotlin.plugin.interakt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("interakt"),
  )
)

publishing {
  elideTarget(
    project,
    label = "Elide Substrate: Interakt Plugin",
    group = "dev.elide.tools.kotlin.plugin",
    artifact = "interakt-plugin",
    summary = "Experimental Kotlin compiler plugin. Coming soon.",
  )
}

dependencies {
  implementation(project(":compiler-util"))
  implementation(libs.kotlin.compiler.embedded)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(project(":compiler-util", "test"))
}
