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
  id("com.gradle.enterprise") version("3.11.4")
}

val micronautVersion: String by settings

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
      from(files("./gradle/elide.versions.toml"))
    }
    create("mnLibs") {
      from("io.micronaut:micronaut-bom:$micronautVersion")
    }
  }
}

rootProject.name = "elide"

include(
//  ":benchmarks",
  ":packages:base",
  ":packages:bom",
  ":packages:frontend",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:platform",
  ":packages:proto",
  ":packages:rpc-js",
  ":packages:rpc-jvm",
  ":packages:server",
  ":packages:ssg",
  ":packages:test",
  ":tools:bundler",
  ":tools:processor",
  ":tools:reports",
)

val buildDocs: String by settings
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

if (buildDocs == "true") {
  include(
    ":site:docs:frontend",
    ":site:docs:node",
    ":site:docs:server",
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

val cacheUsername: String? by settings
val cachePassword: String? by settings
val cachePush: String? by settings

buildCache {
  local {
    isEnabled = !(System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: false)
  }
  remote<HttpBuildCache> {
    isEnabled = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: false
    isPush = (cachePush ?: System.getenv("GRADLE_CACHE_PUSH")) == "true"
    url = uri("https://buildcache.dyme.cloud/gradle/cache/")
    credentials {
      username = cacheUsername ?: System.getenv("GRADLE_CACHE_USERNAME") ?: error("Failed to resolve cache username")
      password = cachePassword ?: System.getenv("GRADLE_CACHE_PASSWORD") ?: error("Failed to resolve cache password")
    }
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
