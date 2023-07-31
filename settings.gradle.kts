@file:Suppress(
  "UnstableApiUsage",
)

import build.less.plugin.settings.buildless

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    google()
  }
}

plugins {
  id("build.less") version("1.0.0-beta1")
  id("com.gradle.enterprise") version("3.14")
//  id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val micronautVersion: String by settings
val embeddedCompose: String by settings
val embeddedR8: String by settings

dependencyResolutionManagement {
  repositoriesMode.set(
    RepositoriesMode.PREFER_PROJECT
  )
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    mavenCentral()
    google()
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

// 1: Gradle convention plugins.
includeBuild("tools/conventions") {
  dependencySubstitution {
    substitute(module("dev.elide.tools:elide-convention-plugins")).using(project(":"))
  }
}

// 2: Kotlin Compiler substrate.
includeBuild("tools/substrate") {
  dependencySubstitution {
    substitute(module("dev.elide.tools:elide-substrate")).using(project(":"))
    substitute(module("dev.elide.tools:elide-substrate-bom")).using(project(":bom"))
    substitute(module("dev.elide.tools:compiler-util")).using(project(":compiler-util"))
    substitute(module("dev.elide.tools.kotlin.plugin:injekt-plugin")).using(project(":injekt"))
    substitute(module("dev.elide.tools.kotlin.plugin:interakt-plugin")).using(project(":interakt"))
    substitute(module("dev.elide.tools.kotlin.plugin:redakt-plugin")).using(project(":redakt"))
    substitute(module("dev.elide.tools.kotlin.plugin:sekret-plugin")).using(project(":sekret"))
  }
}

// 3: Third-party modules.
if (embeddedR8 == "true") includeBuild("tools/third_party/google/r8")
if (embeddedCompose == "true") includeBuild("tools/third_party/jetbrains/compose/web")

// 4: Build modules.
include(
  ":packages:base",
  ":packages:bom",
  ":packages:core",
  ":packages:cli",
  ":packages:frontend",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-py",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:platform",
  ":packages:proto:proto-core",
  ":packages:proto:proto-capnp",
  ":packages:proto:proto-flatbuffers",
  ":packages:proto:proto-kotlinx",
  ":packages:proto:proto-protobuf",
  ":packages:rpc",
  ":packages:server",
  ":packages:ssg",
  ":packages:ssr",
  ":packages:test",
  ":tools:bundler",
  ":tools:processor",
  ":tools:reports",
)

val buildDocs: String by settings
val buildDocsSite: String by settings
val buildSamples: String by settings
val buildPlugins: String by settings
val buildBenchmarks: String by settings

if (buildPlugins == "true") {
  includeBuild(
    "tools/plugin/gradle-plugin",
  )
}

if (buildSamples == "true") {
  include(
    ":samples:server:helloworld",
    ":samples:server:hellocss",
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
    ":docs:architecture",
    ":docs:guide",
  )
}

if (buildDocsSite == "true") {
  include(
    ":site:docs:content",
    ":site:docs:ui",
    ":site:docs:node",
    ":site:docs:app",
  )
}

if (buildBenchmarks == "true") {
  includeBuild("benchmarks")
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
val remoteCache = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: true
val localCache = System.getenv("GRADLE_CACHE_LOCAL")?.toBoolean() ?: true

buildless {
  // nothing to configure at this time
}

// buildCache {
//   local {
//     isEnabled = false
//   }
//   remote<HttpBuildCache> {
//     url = uri("https://gradle.less.build/cache/generic")
//     isEnabled = true
//     isPush = true
//     isUseExpectContinue = true
//     credentials {
//       username = "apikey"
//       password = System.getenv("BUILDLESS_APIKEY")
//     }
//   }
// }

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
