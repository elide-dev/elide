@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://maven.pkg.st/")
  }
}

plugins {
  id("com.gradle.enterprise") version("3.14")
}

dependencyResolutionManagement {
  repositoriesMode.set(
    RepositoriesMode.PREFER_PROJECT
  )
  repositories {
    maven("https://maven.pkg.st/")
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
