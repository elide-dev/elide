import Elide
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

testlogger {
  showStandardStreams = true
}

projectConstants(
  packageName = "elide.tools.kotlin.plugin.redakt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("redakt"),
    "PLUGIN_VERSION" to Constant.string(Elide.version),
  )
)

publishing {
  elideTarget(
    project,
    label = "Elide Substrate: Redakt Plugin",
    group = "dev.elide.tools.kotlin.plugin",
    artifact = "redakt-plugin",
    summary = "Kotlin compiler plugin for redacting sensitive data from logs and toString.",
  )
}

dependencies {
  ksp(libs.autoService.ksp)
  api(project(":compiler-util"))
  compileOnly(libs.kotlin.compiler.embedded)
  implementation(libs.google.auto.service)

  testImplementation(kotlin("test"))
  testImplementation(libs.truth)
  testImplementation(libs.truth.proto)
  testImplementation(libs.truth.java8)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(project(":compiler-util", "test"))
}
