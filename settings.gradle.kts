/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
  "DSL_SCOPE_VIOLATION",
)

pluginManagement {
  repositories {
    maven {
      name = "pkgst-gradle"
      url = uri("https://gradle.pkg.st")
    }
    maven {
      name = "pkgst-maven"
      url = uri("https://maven.pkg.st")
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
      }
    }
    maven {
      name = "elide-snapshots"
      url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
      content {
        includeGroup("dev.elide")
        includeGroup("dev.elide.tools")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.jetbrains.reflekt")
      }
    }
    gradlePluginPortal()
    google()
    mavenCentral()
  }

  if (extra["buildPlugins"] == "true") {
    includeBuild("third_party/apple/pkl")
    includeBuild("tools/plugin/gradle-plugin")
  }
}

plugins {
  id("build.less") version ("1.0.0-rc2")
  id("com.gradle.enterprise") version ("3.16.2")
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version ("1.12.1")
  id("io.micronaut.platform.catalog") version (extra.properties["micronautVersion"] as String)
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val buildUuid: String by settings
val buildlessApiKey: String by settings
val enableSubstrate: String by settings

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT

  repositories {
    maven {
      name = "pkgst-maven"
      url = uri("https://maven.pkg.st")
    }
    maven {
      name = "jpms-modules"
      url = uri("https://jpms.pkg.st/repository")
      content {
        includeGroup("dev.javamodules")
      }
    }
    maven {
      name = "elide-snapshots"
      url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
        includeGroup("org.jetbrains.reflekt")
        includeGroup("com.google.devtools.ksp")
      }
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
      }
    }
    maven {
      name = "dokka-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
      content {
        includeGroup("org.jetbrains.dokka")
      }
    }
    maven {
      name = "gvm-dev"
      url = uri("https://dl.less.build/repository/gvm-edge")
      content {
        includeGroupAndSubgroups("org.graalvm")
        includeGroupByRegex("org.graalvm.*")

        listOf(
          "org.graalvm.ruby",
          "org.graalvm.llvm",
          "org.graalvm.python",
          "org.graalvm.js",
          "org.graalvm.polyglot",
          "org.graalvm.tools",
        ).forEach {
          includeGroupAndSubgroups(it)
        }
      }
    }
    maven {
      name = "wasm-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
      content {
        includeGroup("io.ktor")
        includeGroup("org.jetbrains.compose")
        includeGroup("org.jetbrains.compose.compiler")
        includeGroup("org.jetbrains.kotlin")
        includeGroup("org.jetbrains.kotlinx")
      }
    }
    maven {
      name = "compose-dev"
      url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
      content {
        includeGroup("androidx.compose")
        includeGroup("androidx.compose.compiler")
        includeGroup("org.jetbrains.compose")
        includeGroup("org.jetbrains.compose.compiler")
        includeGroup("web")
      }
    }
    maven {
      name = "compose-edge"
      url = uri("https://androidx.dev/storage/compose-compiler/repository/")
      content {
        includeGroup("androidx.compose")
        includeGroup("androidx.compose.compiler")
        includeGroup("org.jetbrains.compose")
        includeGroup("org.jetbrains.compose.compiler")
      }
    }
    mavenCentral()
    google()
  }
  versionCatalogs {
    create("libs") {
      from(files("./gradle/elide.versions.toml"))
    }
    create("attic") {
      from("dev.javamodules:jpms-catalog:1.0.9")
    }
  }
}

rootProject.name = "elide"

// External builds.
if (buildUuid == "true") {
  includeBuild("packages/uuid") {
    dependencySubstitution {
      substitute(module("dev.elide:uuid")).using(project(":subprojects:uuid-core"))
      substitute(module("dev.elide:elide-uuid")).using(project(":subprojects:uuid-core"))
      substitute(module("dev.elide:elide-uuid-kotlinx")).using(project(":subprojects:uuid-kotlinx"))
    }
  }
}

if (enableSubstrate == "true") {
  includeBuild("tools/conventions")
  includeBuild("tools/substrate") {
    dependencySubstitution {
      substitute(module("dev.elide.tools:compiler-util")).using(project(":compiler-util"))
      substitute(module("dev.elide.tools.kotlin.plugin:redakt-plugin")).using(project(":redakt"))
    }
  }
}

// Build modules.
include(
  ":packages:base",
  ":packages:bom",
  ":packages:cli",
  ":packages:cli-bridge",
  ":packages:core",
  ":packages:embedded",
  ":packages:frontend",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-ts",
  ":packages:graalvm-jvm",
  ":packages:graalvm-llvm",
  ":packages:graalvm-wasm",
  ":packages:graalvm-py",
  ":packages:graalvm-rb",
  ":packages:graalvm-kt",
  ":packages:graalvm-java",
  ":packages:graalvm-react",
  ":packages:http",
  ":packages:model",
  ":packages:nfi",
  ":packages:platform",
  ":packages:proto:proto-core",
  ":packages:proto:proto-test",
  ":packages:proto:proto-capnp",
  ":packages:proto:proto-kotlinx",
  ":packages:proto:proto-protobuf",
  ":packages:runtime",
  ":packages:server",
  ":packages:serverless",
  ":packages:ssr",
  ":packages:test",
  ":packages:wasm",
  ":tools:esbuild",
  ":tools:tsc",
  ":tools:processor",
  ":tools:umbrella",
  ":tools:reports",
  ":tools:wrappers",
)

val buildDocs: String by settings
val buildDocsModules: String by settings
val buildDocsSite: String by settings
val buildSamples: String by settings
val buildPlugins: String by settings
val buildBenchmarks: String by settings
val buildRpc: String by settings
val buildFlatbuffers: String by settings

includeBuild(
  "tools/elide-build",
)

if (buildSamples == "true") {
  includeBuild("samples")
}

if (buildRpc == "true") include(":packages:rpc")

if (buildFlatbuffers == "true") include(":packages:proto:proto-flatbuffers")

if (buildDocs == "true" && buildDocsModules == "true") {
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

val cachePush: String? by settings
val isCI: Boolean = System.getenv("CI") != "true"

buildless {
  remoteCache {
    enabled = true

    // allow disabling pushing to the remote cache
    push.set(cachePush?.toBooleanStrictOrNull() ?: true)
  }
  localCache {
    enabled = true
  }
}

buildCache {
  local {
    isEnabled = true
    directory = layout.rootDirectory.dir(".codebase/build-cache")
  }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
