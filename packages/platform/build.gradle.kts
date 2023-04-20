
plugins {
  `maven-publish`
  `java-platform`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

group = "dev.elide"
version = rootProject.version as String


// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to Versions.guava),
  "protobuf" to ("com.google.protobuf:protobuf-java" to Versions.protobuf),
  "grpc" to ("io.grpc:grpc-bom" to Versions.grpc),
  "netty" to ("io.netty:netty-bom" to Versions.netty),
  "micronaut" to ("io.micronaut:micronaut-bom" to Versions.micronaut),
)

kover {
  isDisabled.set(true)
}

dependencies {
  constraints {
    // BOMs: gRPC, Netty, Micronaut.
    api(libs.grpc.bom)
    api(libs.netty.bom)
    api(libs.micronaut.bom)
    api(libs.projectreactor.bom)

    // Kotlin.
    api(kotlin("stdlib"))

    // Google: Protocol Buffers, Guava, GAX, gRPC.
    api(libs.guava)
    api(libs.protobuf.java)
    api(libs.protobuf.kotlin)

    // KotlinX: Co-routines.
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.core.jvm)
    api(libs.kotlinx.collections.immutable)

    // KotlinX: Datetime.
    api(libs.kotlinx.datetime)

    // KotlinX: Serialization.
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.core.jvm)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.json.jvm)
    api(libs.kotlinx.serialization.protobuf)
    api(libs.kotlinx.serialization.protobuf.jvm)
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["javaPlatform"])
      groupId = "dev.elide"
      artifactId = "elide-platform"
      version = project.version as String
    }
  }
}

sonarqube {
  isSkipProject = true
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = "elide-platform"

    pom {
      name.set("Elide Platform")
      url.set("https://elide.dev")
      description.set(
        "Elide Platform and catalog for Gradle."
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
