dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    gradlePluginPortal()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
  }
}

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://maven.pkg.st/")
  }
}

//plugins {
//  id("dev.elide.build.jvm.toolchains")
//}

rootProject.name = "elideBuild"
