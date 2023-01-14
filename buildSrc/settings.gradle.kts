dependencyResolutionManagement {
  repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
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
    mavenCentral()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
}

//plugins {
//  id("dev.elide.build.jvm.toolchains")
//}

rootProject.name = "elideBuild"
