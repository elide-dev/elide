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
      name.set("Elide Python for GraalVM")
      url.set("https://elide.dev")
      description.set(
        "Integration package with GraalVM and GraalPy."
      )

      licenses {
        license {
          name.set("MIT License")
          url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
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
        url.set("https://github.com/elide-dev/elide")
      }
    }
  }
}
