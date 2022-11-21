@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  publishing
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("io.micronaut.graalvm")
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String

dependencies {
  implementation(platform(project(":packages:platform")))
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))
  implementation(libs.picocli)
  implementation(libs.micronaut.picocli)
}

application {
  mainClass.set("elide.tool.ssg.SiteCompiler")
}

sonarqube {
  isSkipProject = true
}
