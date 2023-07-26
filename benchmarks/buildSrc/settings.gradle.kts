dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "benchmarksBuild"
