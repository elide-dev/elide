dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven {
      url = uri("https://maven.pkg.github.com/elide-dev/elide")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
  }
}

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
}

rootProject.name = "elideBuild"
