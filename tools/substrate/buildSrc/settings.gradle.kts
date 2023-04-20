@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.pkg.st/")
  }
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "substrateBuild"
