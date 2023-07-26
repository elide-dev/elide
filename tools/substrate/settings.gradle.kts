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
  repositories {
    maven("https://maven.pkg.st/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "substrate"

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

include(
  ":bom",
  ":compiler-util",
  ":injekt",
  ":interakt",
  ":redakt",
  ":sekret",
)
