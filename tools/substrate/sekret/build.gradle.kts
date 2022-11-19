import GradleProject.projectConstants

plugins {
  id("dev.elide.build")
  id("dev.elide.build.kotlin.compilerPlugin")
}


project.projectConstants(
  packageName = "elide.tools.kotlin.plugin.sekret",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("sekret"),
  )
)

dependencies {
  implementation(project(":compiler-util"))
  implementation(libs.kotlin.compiler.embedded)

  testImplementation(kotlin("test"))
  testApi(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(project(":compiler-util", "test"))
}
