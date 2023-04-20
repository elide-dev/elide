import Elide

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
  "elide-base",
  "elide-core",
  "elide-test",
  "elide-proto-core",
  "elide-proto-flatbuffers",
  "elide-proto-protobuf",
  "elide-proto-kotlinx",
  "elide-ssr",
  "elide-graalvm",
  "elide-graalvm-js",
  "elide-graalvm-react",
  "elide-model",
  "elide-server",
  "elide-frontend",
  "elide-rpc",
  "elide-ssg",
)

// Peer modules.
val peers = mapOf(
  "guava" to ("com.google.guava:guava" to libs.versions.guava.get()),
  "protobuf" to ("com.google.protobuf:protobuf-java" to libs.versions.protobuf.get()),
  "grpc" to ("io.grpc:grpc-bom" to libs.versions.grpc.java.get()),
  "micronaut" to ("io.micronaut:micronaut-bom" to libs.versions.micronaut.lib.get()),
)

kover {
  isDisabled.set(true)
}

catalog {
  versionCatalog {
    // map Elide versions
    version("elide", libs.versions.elide.asProvider().get())
    version("elidePlugin", libs.versions.elide.plugin.get())

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
