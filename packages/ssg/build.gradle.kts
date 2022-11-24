@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  `java-library`
  publishing
  jacoco

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

val testProject = ":samples:server:helloworld"

val embeddedJars by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false
}

dependencies {
  kapt(libs.micronaut.inject.java)
  implementation(libs.commons.compress)
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

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.micronaut.test.junit5)
  testImplementation(project(testProject))
  embeddedJars(project(
    testProject,
    configuration = "shadowAppJar",
  ))
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

tasks.test {
  useJUnitPlatform()
  systemProperty("elide.test", "true")
  systemProperty("tests.buildDir", "${project.buildDir}/ssgTests/")
  systemProperty("tests.exampleManifest", project.buildDir.resolve("resources/test/app.manifest.pb"))
  systemProperty("tests.textManifest", project.buildDir.resolve("resources/test/example-manifest.txt.pb"))
  systemProperty("tests.invalidManifest", project.buildDir.resolve("resources/test/example-invalid.txt.pb"))
}
