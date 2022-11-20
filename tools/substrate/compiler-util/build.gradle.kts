plugins {
  `maven-publish`
  distribution
  signing

  id("java")
  id("java-test-fixtures")
  id("dev.elide.build")
  kotlin("jvm")
  kotlin("kapt")
  id("dev.elide.build.substrate")
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

publishing {
  publications.withType<MavenPublication> {
    pom {
      name.set("Elide Substrate: Compiler Utilities")
      url.set("https://github.com/elide-dev/v3")
      description.set(
        "Provides utilities for Elide Kotlin Compiler plugins."
      )

      licenses {
        license {
          name.set("MIT License")
          url.set("https://github.com/elide-dev/v3/blob/v3/LICENSE")
        }
      }
      developers {
        developer {
          id.set("sgammon")
          name.set("Sam Gammon")
          email.set("samuel.gammon@gmail.com")
        }
      }
      scm {
        url.set("https://github.com/elide-dev/v3")
      }
    }
  }
}
