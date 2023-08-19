/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *     https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress(
  "UnstableApiUsage",
)

import build.less.plugin.settings.buildless

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    gradlePluginPortal()
    google()
  }
}

plugins {
  id("build.less") version("1.0.0-beta1")
  id("com.gradle.enterprise") version("3.14.1")
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version("1.11.1")
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val micronautVersion: String by settings
val embeddedCompose: String by settings
val embeddedR8: String by settings
val buildUuid: String by settings

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
    mavenCentral()
    google()
  }
  versionCatalogs {
    create("libs") {
      from(files("./gradle/elide.versions.toml"))
    }
    create("mn") {
      from("io.micronaut.platform:micronaut-platform:$micronautVersion")
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
    substitute(module("dev.elide.tools.kotlin.plugin:redakt-plugin")).using(project(":redakt"))
  }
}

// 3: Third-party modules.
if (embeddedR8 == "true") includeBuild("tools/third_party/google/r8")
if (embeddedCompose == "true") includeBuild("tools/third_party/jetbrains/compose/web")

// 4: External builds.
if (buildUuid == "true") {
  includeBuild("packages/uuid") {
    dependencySubstitution {
      substitute(module("dev.elide:elide-uuid")).using(project(":"))
    }
  }
}

// 5: Build modules.
include(
  ":packages:base",
  ":packages:bom",
  ":packages:core",
  ":packages:runtime-core",
  ":packages:runtime-js",
  ":packages:frontend",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-jvm",
  ":packages:graalvm-llvm",
  ":packages:graalvm-wasm",
  ":packages:graalvm-py",
  ":packages:graalvm-rb",
  ":packages:graalvm-kt",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:platform",
  ":packages:runtime",
  ":packages:proto:proto-core",
  ":packages:proto:proto-capnp",
  ":packages:proto:proto-flatbuffers",
  ":packages:proto:proto-kotlinx",
  ":packages:proto:proto-protobuf",
  ":packages:server",
  ":packages:ssr",
  ":packages:test",
  ":tools:processor",
  ":tools:reports",
  ":tools:wrappers",
  ":packages:cli",
)

val buildDocs: String by settings
val buildDocsSite: String by settings
val buildSamples: String by settings
val buildPlugins: String by settings
val buildBenchmarks: String by settings
val buildRpc: String by settings
val buildSsg: String by settings
val buildWasm: String by settings

if (buildWasm == "true") {
  include(":packages:wasm")
}

if (buildPlugins == "true") {
  includeBuild(
    "tools/plugin/gradle-plugin",
  )
}

if (buildSsg == "true") {
  include(
    ":packages:ssg",
    ":tools:bundler",
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

if (buildRpc == "true") include(":packages:rpc")

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
  include(
    ":benchmarks:core",
    ":benchmarks:graalvm",
    ":benchmarks:server",
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
val remoteCache = System.getenv("GRADLE_CACHE_REMOTE")?.toBoolean() ?: true
val localCache = System.getenv("GRADLE_CACHE_LOCAL")?.toBoolean() ?: true

buildless {
  remoteCache {
    // allow disabling pushing to the remote cache
    push = cachePush?.toBooleanStrictOrNull() ?: true
  }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
