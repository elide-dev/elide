@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.tool")
//  id("io.micronaut.application")
//  id("io.micronaut.aot")
}

dependencies {
  api(platform(project(":packages:platform")))
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))
  implementation(libs.picocli)
  implementation(libs.micronaut.picocli)
}

//application {
//  mainClass.set("elide.tool.ssg.SiteCompiler")
//}

sonarqube {
  isSkipProject = true
}
