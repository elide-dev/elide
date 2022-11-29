pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.11.4")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

rootProject.name = "elide-gradle-plugin"

include(
    ":example:fullstack:browser",
    ":example:fullstack:node",
    ":example:fullstack:server",
    ":example:static:frontend",
    ":example:static:server",
    ":example:mixed",
)
includeBuild("plugin-build")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
