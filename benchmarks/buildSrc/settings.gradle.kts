dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "benchmarksBuild"
