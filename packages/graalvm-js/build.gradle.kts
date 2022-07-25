@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import java.net.URI

plugins {
  idea
  `maven-publish`
  signing
  kotlin("js")
  kotlin("plugin.serialization")
  alias(libs.plugins.dokka)
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  js {
    nodejs()
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = "graalvm-js"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

signing {
  if (project.hasProperty("enableSigning") && project.properties["enableSigning"] == "true") {
    sign(configurations.archives.get())
    sign(publishing.publications)
  }
}

publishing {
  repositories {
    maven {
      name = "elide"
      url = URI.create(project.properties["elide.publish.repo.maven"] as String)

      if (project.hasProperty("elide.publish.repo.maven.auth")) {
          credentials {
              username = (project.properties["elide.publish.repo.maven.username"] as? String
                  ?: System.getenv("PUBLISH_USER"))?.ifBlank { null }
              password = (project.properties["elide.publish.repo.maven.password"] as? String
                  ?: System.getenv("PUBLISH_TOKEN"))?.ifBlank { null }
          }
      }
    }
  }

  publications.withType<MavenPublication> {
    pom {
      name.set("Elide")
      description.set("Polyglot application framework")
      url.set("https://github.com/elide-dev/v3")

      licenses {
        license {
          name.set("Properity License")
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

dependencies {
  api(npm("esbuild", libs.versions.npm.esbuild.get()))
  api(npm("prepack", libs.versions.npm.prepack.get()))
  api(npm("buffer", libs.versions.npm.buffer.get()))
  api(npm("readable-stream", libs.versions.npm.stream.get()))

  implementation(libs.kotlinx.wrappers.node)

  // Testing
  testImplementation(project(":packages:test"))
}
