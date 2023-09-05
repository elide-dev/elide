@file:Suppress("UnstableApiUsage")

rootProject.name = "elide-internal-plugin"

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://maven.pkg.st/")
  }
}

plugins {
  id("com.gradle.enterprise") version("3.14.1")
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    google()
  }
  
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
