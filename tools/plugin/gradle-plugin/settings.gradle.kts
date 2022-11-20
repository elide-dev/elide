pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        google()
        maven("https://plugins.gradle.org/m2/")
        maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.11.4")
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
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
