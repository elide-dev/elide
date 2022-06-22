
plugins {
  idea
  `maven-publish`
  signing
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
}

kotlin {
  js {
    nodejs()
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = "graalvm-react"
        version = rootProject.version as String ?: "1.0-SNAPSHOT"

        from(components["kotlin"])
      }
    }
  }
}

publishing {
  repositories {
    maven("gcs://elide-snapshots/repository/v3")
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
  implementation(project(":graalvm-js"))
  implementation("org.jetbrains.kotlinx:kotlinx-nodejs:${Versions.nodeDeclarations}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-node:${Versions.node}-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react:${Versions.react}-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:${Versions.react}-${Versions.kotlinWrappers}")
}
