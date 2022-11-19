plugins {
  id("java")
  id("java-test-fixtures")
  id("dev.elide.build")
  kotlin("jvm")
  kotlin("kapt")
}

group = "dev.elide.tools"
version = rootProject.version as String

kotlin {
  explicitApi()
}

val test by configurations.creating

dependencies {
  kapt(libs.google.auto.service)
  api(libs.google.auto.service.annotations)
  implementation(libs.kotlin.compiler.embedded)

  testApi(kotlin("test"))
  testApi(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.embedded)
}



val testArchive by tasks.registering(Jar::class) {
  archiveBaseName.set("tests")
  from(sourceSets["test"].output)
}

artifacts {
  add("test", testArchive)
}

