@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
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
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
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

include(":server")
