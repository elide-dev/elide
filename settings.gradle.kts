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
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
      }
    }
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("dev.elide.tools")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.jetbrains.reflekt")
        includeGroup("org.pkl-lang")
      }
    }
    maven {
      name = "gvm-plugin-snapshots"
      url = uri("https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots")
      content {
        includeGroup("org.graalvm.buildtools")
      }
    }
    maven {
      name = "jpms-modules"
      url = uri("https://jpms.pkg.st/repository")
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }

  includeBuild("tools/elide-build")
}

plugins {
  id("com.gradle.enterprise") version ("3.16.2")
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version ("2.1")
  id("io.micronaut.platform.catalog") version (extra.properties["micronautCatalogVersion"] as String)
  id("elide.toolchains.jvm")
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val buildUuid: String by settings
val buildAuxImage: String by settings
val enableSubstrate: String by settings

toolchainManagement {
  jvm {
    javaRepositories {
      repository("gvm-edge") {
        resolverClass = elide.toolchain.jvm.JvmToolchainResolver::class.java
      }
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.PREFER_PROJECT

  repositories {
    maven {
      name = "jpms-modules"
      url = uri("https://jpms.pkg.st/repository")
    }
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
        includeGroup("net.java.dev.jna")
        includeGroup("org.jetbrains.reflekt")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.pkl-lang")
        includeGroupAndSubgroups("org.graalvm")
        includeGroupByRegex("org.graalvm.*")

        listOf(
          "org.graalvm.ruby",
          "org.graalvm.llvm",
          "org.graalvm.python",
          "org.graalvm.js",
          "org.graalvm.polyglot",
          "org.graalvm.tools",
        ).forEach(::includeGroupAndSubgroups)
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
    maven {
      name = "sonatype-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
    google()
  }
  versionCatalogs {
    create("libs") {
      from(files("./gradle/elide.versions.toml"))
    }
    create("attic") {
      from("dev.javamodules:jpms-catalog:1.0.10")
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

// Auxiliary image builder.
if (buildAuxImage == "true") {
  include(
    ":tools:auximage",
  )
}

// Build modules.
include(
  ":crates:base",
  ":crates:builder",
  ":crates:compression",
  ":crates:deps",
  ":crates:diag",
  ":crates:entry",
  ":crates:model",
  ":crates:js",
  ":crates:posix",
  ":crates:project",
  ":crates:protocol",
  ":crates:sqlite",
  ":crates:substrate",
  ":crates:terminal",
  ":crates:transport",
  ":packages:base",
  ":packages:bom",
  ":packages:cli",
  ":packages:core",
  ":packages:engine",
  ":packages:graalvm",
  ":packages:graalvm-java",
  ":packages:graalvm-jvm",
  ":packages:graalvm-js",
  ":packages:graalvm-kt",
  ":packages:graalvm-llvm",
  ":packages:graalvm-py",
  ":packages:graalvm-rb",
  ":packages:graalvm-ts",
  ":packages:graalvm-wasm",
  ":packages:http",
  ":packages:platform",
  ":packages:proto:proto-capnp",
  ":packages:proto:proto-core",
  ":packages:proto:proto-kotlinx",
  ":packages:proto:proto-protobuf",
  ":packages:proto:proto-test",
  ":packages:server",
  ":packages:sqlite",
  ":packages:ssr",
  ":packages:tcnative",
  ":packages:terminal",
  ":packages:test",
  ":packages:tooling",
  ":packages:transport:transport-common",
  ":packages:transport:transport-epoll",
  ":packages:transport:transport-kqueue",
  ":packages:transport:transport-unix",
  ":packages:transport:transport-uring",
  ":tools:reports",
  ":tools:umbrella",
)

val buildDeprecated: String by settings
val buildDocs: String by settings
val buildEmbedded: String by settings
val buildDocsModules: String by settings
val buildSamples: String by settings
val buildPlugins: String by settings
val buildBenchmarks: String by settings
val buildFlatbuffers: String by settings

if (buildSamples == "true") {
  includeBuild("samples")
}
if (buildEmbedded == "true") {
  include(
    ":packages:embedded",
  )
}

if (buildPlugins == "true") {
  includeBuild("tools/plugin/gradle-plugin")
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

val isCI: Boolean = System.getenv("CI") != "true"

//
// -- Begin Caching Tricks
//

val inertTasks = sortedSetOf("clean", "tasks", "projects")
if (gradle.startParameter.taskNames.size == 1 && gradle.startParameter.taskNames.first() in inertTasks) {
  // disable the build cache when cleaning because having it on is just silly
  buildCache {
    local.isEnabled = false
    remote?.isEnabled = false
  }

  // force-disable
  gradle.startParameter.isBuildCacheEnabled = false
}

//
// -- End Caching Tricks
//

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
