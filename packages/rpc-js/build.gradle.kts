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
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.dokka)
  alias(libs.plugins.sonar)
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  js {
    browser()
    nodejs()
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = "rpc-js"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
}

signing {
  if (project.hasProperty("enableSigning") && project.properties["enableSigning"] == "true") {
    sign(configurations.archives.get())
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
    artifact(javadocJar.get())
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
  implementation(project(":packages:base"))
  implementation(project(":packages:frontend"))
  implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
  implementation(npm("google-protobuf", libs.versions.protobuf.get()))
  implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.serialization.protobuf.js)

  // Testing
  testImplementation(project(":packages:test"))
}

tasks.dokkaHtml.configure {
  moduleName.set("rpc-js")
}
