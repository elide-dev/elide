plugins {
  id("java")
  id("java-test-fixtures")
  id("dev.elide.build")
  id("dev.elide.build.jvm.kapt")
}

kotlin {
  explicitApi()
}

val test by configurations.creating

dependencies {
  kapt(libs.google.auto.service)
  api(platform(project(":packages:platform")))
  api(libs.google.auto.service.annotations)
  implementation(libs.kotlin.compiler.embedded)

  testImplementation(kotlin("test"))
  testImplementation(libs.kotlin.compiler.embedded)
}

val testArchive by tasks.registering(Jar::class) {
  archiveBaseName.set("tests")
  from(sourceSets["test"].output)
}

artifacts {
  add("test", testArchive)
}

