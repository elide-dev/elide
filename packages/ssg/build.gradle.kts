@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  `java-library`
  publishing

  kotlin("plugin.serialization")
  id("com.github.gmazzo.buildconfig")
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("dev.elide.build.jvm.kapt")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()
}

buildConfig {
  className("ElideSSGCompiler")
  packageName("elide.tool.ssg.cfg")
  useKotlinOutput()

  buildConfigField("String", "ELIDE_TOOL_VERSION", "\"${libs.versions.elide.asProvider().get()}\"")
}

dependencies {
  kapt(libs.micronaut.inject.java)
  implementation(platform(project(":packages:platform")))
  implementation(project(":packages:base"))
  implementation(project(":packages:proto"))
  implementation(project(":packages:server"))
  implementation(libs.jsoup)
  implementation(libs.picocli)
  implementation(libs.micronaut.picocli)
  implementation(libs.micronaut.http.client)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.reactive)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.serialization.protobuf)
}

application {
  mainClass.set("elide.tool.ssg.SiteCompiler")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}

sonarqube {
  isSkipProject = true
}
