@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()

  sourceSets {
    val jsMain by getting {
      dependencies {
        api(project(":packages:ssr"))
        api(npm("esbuild", libs.versions.npm.esbuild.get()))
        api(npm("prepack", libs.versions.npm.prepack.get()))
        api(npm("buffer", libs.versions.npm.buffer.get()))
        api(npm("readable-stream", libs.versions.npm.stream.get()))
        api(npm("web-streams-polyfill", libs.versions.npm.webstreams.get()))
        api(npm("@emotion/css", libs.versions.npm.emotion.core.get()))
        api(npm("@emotion/server", libs.versions.npm.emotion.server.get()))

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.wrappers.node)
        implementation(libs.kotlinx.wrappers.emotion)
        implementation(libs.kotlinx.wrappers.history)
        implementation(libs.kotlinx.wrappers.typescript)
        implementation(libs.kotlinx.wrappers.react.router.dom)
        implementation(libs.kotlinx.wrappers.remix.run.router)
      }
    }

    val jsTest by getting {
      dependencies {
        // Testing
        implementation(project(":packages:test"))
      }
    }
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("graalvm", "elide-graalvm")

    pom {
      name.set("Elide JavaScript for GraalVM")
      url.set("https://elide.dev")
      description.set(
        "Integration package with GraalVM and GraalJS."
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
