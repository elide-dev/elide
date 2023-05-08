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

group = "dev.elide"
version = rootProject.version as String

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
    group = "dev.elide",
    artifact = "kotlin-interakt-plugin",
    summary = "Experimental Kotlin compiler plugin. Coming soon.",
  )
}

dependencies {
  ksp(libs.autoService.ksp)
  api(project(":kotlin-compiler-util"))
  compileOnly(libs.kotlin.compiler.embedded)
  implementation(libs.google.auto.service)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.testing)
  testImplementation(libs.kotlin.compiler.embedded)
  testImplementation(project(":kotlin-compiler-util", "test"))
}
