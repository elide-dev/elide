import java.net.URI

plugins {
  idea
  `maven-publish`
  signing
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  id("org.sonarqube")
  id("org.jetbrains.dokka")
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
  api(npm("esbuild", Versions.esbuild))
  api(npm("esbuild-plugin-alias", Versions.esbuildPluginAlias))
  api(npm("buffer", Versions.nodeBuffers))
  api(npm("readable-stream", Versions.nodeStreams))
  implementation("org.jetbrains.kotlinx:kotlinx-nodejs:${Versions.nodeDeclarations}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-node:${Versions.node}-${Versions.kotlinWrappers}")

  // Testing
  testImplementation(project(":packages:test"))
}
