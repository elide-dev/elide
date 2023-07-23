@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.pkg.st/")
  }
}

plugins {
  id("com.gradle.enterprise") version("3.14")
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    gradlePluginPortal()
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    google()
  }
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "elide-convention-plugins"

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
