@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js.node")
}

group = "dev.elide"
version = rootProject.version as String

dependencies {
  api(npm("esbuild", libs.versions.npm.esbuild.get()))
  api(npm("prepack", libs.versions.npm.prepack.get()))
  api(npm("buffer", libs.versions.npm.buffer.get()))
  api(npm("readable-stream", libs.versions.npm.stream.get()))

  implementation(project(":packages:graalvm-js"))

  implementation(libs.kotlinx.wrappers.node)
  implementation(libs.kotlinx.wrappers.mui)
  implementation(libs.kotlinx.wrappers.react)
  implementation(libs.kotlinx.wrappers.react.dom)
  implementation(libs.kotlinx.wrappers.react.router.dom)
  implementation(libs.kotlinx.wrappers.remix.run.router)
  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.kotlinx.wrappers.emotion)
  implementation(libs.kotlinx.wrappers.browser)
  implementation(libs.kotlinx.wrappers.history)
  implementation(libs.kotlinx.wrappers.typescript)

  // Testing
  testImplementation(project(":packages:test"))
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("graalvm", "elide-graalvm")

    pom {
      name.set("Elide React integration for GraalJS")
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
