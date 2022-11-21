plugins {
  `maven-publish`
  distribution
  signing

  id("java")
  id("java-test-fixtures")
  kotlin("jvm")
  kotlin("kapt")

  id("dev.elide.build")
  id("dev.elide.build.jvm")
  id("dev.elide.build.kotlin")
  id("dev.elide.build.substrate")
}

group = "dev.elide.tools"
version = rootProject.version as String

kotlin {
  explicitApi()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val buildDocs = (project.properties["buildDocs"] as? String ?: "true") == "true"
val test by configurations.creating

dependencies {
  kapt(libs.google.auto.service)
  api(libs.google.auto.service.annotations)
  implementation(libs.kotlin.compiler.embedded)

  testApi(kotlin("test"))
  testApi(libs.junit.jupiter.api)
  testApi(libs.junit.jupiter.params)
  testApi(libs.kotlin.compiler.testing)
  testApi(libs.truth)
  testApi(libs.truth.proto)
  testApi(libs.truth.java8)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.embedded)
}

val testArchive by tasks.registering(Jar::class) {
  archiveClassifier.set("tests")
  from(sourceSets["test"].output)
}

artifacts {
  add("test", testArchive)
}

publishing {
  publications.create<MavenPublication>("maven") {
    group = "dev.elide.tools"
    artifactId = "compiler-util"
    version = rootProject.version as String
    from(components["kotlin"])
    artifact(tasks["testArchive"])
  }

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
