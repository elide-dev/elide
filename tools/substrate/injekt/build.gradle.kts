import GradleProject.projectConstants
import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  distribution
  signing

  id("dev.elide.build")
  id("dev.elide.build.jvm")
  id("com.google.devtools.ksp")
  id("dev.elide.build.kotlin.compilerPlugin")
}

group = "dev.elide.tools.kotlin.plugin"
version = rootProject.version as String

projectConstants(
  packageName = "elide.tools.kotlin.plugin.injekt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("injekt"),
  )
)

publishing {
  elideTarget(
    project,
    label = "Elide Substrate: Injekt Plugin",
    group = "dev.elide.tools.kotlin.plugin",
    artifact = "injekt-plugin",
    summary = "Kotlin compiler plugin for light dependency injection, with support for multi-platform projects.",
  )
}

dependencies {
  ksp(libs.autoService.ksp)
  api(project(":compiler-util"))
  compileOnly(libs.kotlin.compiler.embedded)
  implementation(libs.google.auto.service)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(project(":compiler-util", "test"))
}
