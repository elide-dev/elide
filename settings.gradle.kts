@file:Suppress(
  "UnstableApiUsage",
)

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("com.gradle.enterprise") version("3.10.2")
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
  }
  versionCatalogs {
    create("libs") {
      from(files("./gradle/elide.versions.toml"))
    }
  }
}

rootProject.name = "elide"

include(
  ":benchmarks:server",
  ":packages:base",
  ":packages:frontend",
  ":packages:server",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:proto",
  ":packages:rpc-js",
  ":packages:rpc-jvm",
  ":packages:test",
  ":tools:bundler",
  ":tools:reports",
)

val buildSamples: String by settings
val buildPlugins: String by settings

if (buildSamples == "true") {
  include(
    ":samples:server:hellocss",
    ":samples:server:helloworld",
    ":samples:fullstack:basic:frontend",
    ":samples:fullstack:basic:server",
    ":samples:fullstack:react:frontend",
    ":samples:fullstack:react:server",
    ":samples:fullstack:ssr:node",
    ":samples:fullstack:ssr:server",
    ":samples:fullstack:react-ssr:frontend",
    ":samples:fullstack:react-ssr:node",
    ":samples:fullstack:react-ssr:server",
  )
}

if (buildPlugins == "true") {
  includeBuild(
    "tools/plugin/gradle-plugin",
  )
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
