
plugins {
  `version-catalog`
  `maven-publish`
  distribution
  signing
  idea

  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

group = "dev.elide"
version = rootProject.version as String

// Elide modules.
val libraries = listOf(
  "base",
  "base-js",
  "base-jvm",
  "server",
  "ssg",
  "frontend",
  "proto",
  "test",
  "rpc-js",
  "rpc-jvm",
  "graalvm",
  "graalvm-js",
  "graalvm-react",
)

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

catalog {
  versionCatalog {
    // map Elide versions
    version("elide", Elide.version)
    version("elidePlugin", Elide.pluginVersion)

    // map each peer version
    peers.forEach { alias, (_, version) ->
      version(alias, version)
    }

    // define Elide build plugin
    plugin("buildtools", "dev.elide.buildtools.plugin").versionRef("elidePlugin")

    // define the BOM (this module)
    library("elide-bom", Elide.group, "bom").versionRef("elide")

    // define Elide library aliases
    libraries.forEach { libName ->
      library("elide-$libName", Elide.group, libName).versionRef("elide")
    }

    // define peer library aliases
    peers.forEach { alias, (group, _) ->
      library(alias, group.split(":").first(), group.split(":").last()).versionRef(
        alias
      )
    }
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "dev.elide"
      artifactId = "bom"
      version = project.version as String
      from(components["versionCatalog"])
    }
  }
}

sonarqube {
  isSkipProject = true
}
