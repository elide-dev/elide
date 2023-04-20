dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    gradlePluginPortal()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "benchmarksBuild"
