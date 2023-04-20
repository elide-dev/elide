@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
  }
}

plugins {
  id("com.gradle.enterprise") version("3.11.4")
}

dependencyResolutionManagement {
  repositoriesMode.set(
    RepositoriesMode.PREFER_PROJECT
  )
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "elide-benchmarks"

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(":graalvm")
include(":server")
