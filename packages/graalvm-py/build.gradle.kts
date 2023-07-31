@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("kapt")
  kotlin("plugin.allopen")
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  // Testing
  testImplementation(project(":packages:test"))
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("graalvm", "elide-graalvm")

    pom {
      name = "Elide Python for GraalVM"
      url = "https://elide.dev"
      description = "Integration package with GraalVM and GraalPy."

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}
