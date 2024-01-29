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
    maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/") {
      content {
        includeGroup("dev.elide")
      }
    }
    gradlePluginPortal()
    google()
    mavenCentral()
  }
  includeBuild("tools/plugin/gradle-plugin")
}

plugins {
  id("build.less") version("1.0.0-rc2")
  id("com.gradle.enterprise") version("3.15.1")
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version("1.12")
  id("io.micronaut.platform.catalog") version(extra.properties["micronautVersion"] as String)
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val embeddedCompose: String by settings
val embeddedR8: String by settings
val buildUuid: String by settings

val buildlessApiKey: String by settings

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT
  repositories {
    maven {
      name = "pkgst-maven"
      url = uri("https://maven.pkg.st")
    }
    maven {
      name = "elide-snapshots"
      url = uri("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
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
  }
}

rootProject.name = "elide"

// Third-party modules.
if (embeddedR8 == "true") includeBuild("tools/third_party/google/r8")
if (embeddedCompose == "true") includeBuild("tools/third_party/jetbrains/compose/web")

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

includeBuild("tools/conventions")
includeBuild("tools/substrate")

// Build modules.
include(
  ":packages:base",
  ":packages:bom",
  ":packages:cli",
  ":packages:core",
  ":packages:frontend",
  ":packages:graalvm",
  ":packages:graalvm-js",
  ":packages:graalvm-jvm",
  ":packages:graalvm-llvm",
  ":packages:graalvm-wasm",
  ":packages:graalvm-py",
  ":packages:graalvm-rb",
  ":packages:graalvm-kt",
  ":packages:graalvm-java",
  ":packages:graalvm-react",
  ":packages:model",
  ":packages:platform",
  ":packages:runtime",
  ":packages:proto:proto-core",
  ":packages:proto:proto-test",
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
)

val buildDocs: String by settings
val buildDocsSite: String by settings
val buildSamples: String by settings
val buildPlugins: String by settings
val buildBenchmarks: String by settings
val buildRpc: String by settings
val buildSsg: String by settings
val buildWasm: String by settings

//if (buildWasm == "true") {
//  include(":packages:wasm")
//}

includeBuild(
  "tools/elide-build",
)

if (buildSamples == "true") include(
  ":samples:server:helloworld",
  ":samples:server:hellocss",
  ":samples:fullstack:basic:frontend",
  ":samples:fullstack:basic:server",
  ":samples:fullstack:ssr:node",
  ":samples:fullstack:ssr:server",
  ":samples:fullstack:react:frontend",
  ":samples:fullstack:react:server",
  ":samples:fullstack:react-ssr:frontend",
  ":samples:fullstack:react-ssr:node",
  ":samples:fullstack:react-ssr:server",
)

if (buildSsg == "true") {
  include(
    ":packages:ssg",
    ":tools:bundler",
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

val cachePush: String? by settings

buildCache {
  local {
    isEnabled = true
  }
}

buildless {
  remoteCache {
    // allow disabling pushing to the remote cache
    push.set(cachePush?.toBooleanStrictOrNull() ?: true)
  }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
