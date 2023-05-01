@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://maven.pkg.st/")
  }
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide.pkg.st/")
    maven("https://gradle.pkg.st/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "substrateBuild"
