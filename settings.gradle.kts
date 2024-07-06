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
      }
    }
    maven {
      name = "gvm-plugin-snapshots"
      url = uri("https://raw.githubusercontent.com/graalvm/native-build-tools/snapshots")
      content {
        includeGroup("org.graalvm.buildtools")
      }
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }

  includeBuild("tools/elide-build")
}

plugins {
  id("build.less") version ("1.0.0-rc2")
  id("com.gradle.enterprise") version ("3.16.2")
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version ("1.12.1")
  id("io.micronaut.platform.catalog") version (extra.properties["micronautVersion"] as String)
  id("elide.toolchains.jvm")
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

val buildUuid: String by settings
val buildPkl: String by settings
val buildAuxImage: String by settings
val buildlessApiKey: String by settings
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
      content {
        includeGroup("dev.javamodules")
      }
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

// Auxiliary image builder.
if (buildAuxImage == "true") {
  include(
    ":tools:auximage",
  )
}

// Embedded languages.
if (buildPkl == "true") {
  includeBuild("third_party/apple/pkl") {
    dependencySubstitution {
      substitute(module("org.pkl-lang:pkl-core")).using(project(":pkl-core"))
      substitute(module("org.pkl-lang:pkl-executor")).using(project(":pkl-executor"))
      substitute(module("org.pkl-lang:pkl-commons-cli")).using(project(":pkl-commons-cli"))
      substitute(module("org.pkl-lang:pkl-codegen-java")).using(project(":pkl-codegen-java"))
      substitute(module("org.pkl-lang:pkl-codegen-kotlin")).using(project(":pkl-codegen-kotlin"))
      substitute(module("org.pkl-lang:pkl-config-java")).using(project(":pkl-config-java"))
      substitute(module("org.pkl-lang:pkl-config-kotlin")).using(project(":pkl-config-kotlin"))
      substitute(module("org.pkl-lang:pkl-tools")).using(project(":pkl-tools"))
      substitute(module("org.pkl-lang:pkl-server")).using(project(":pkl-server"))
      substitute(module("org.pkl-lang:pkl-doc")).using(project(":pkl-doc"))
      substitute(module("org.pkl-lang:pkl-cli")).using(project(":pkl-cli"))
    }
  }
}

// Build modules.
include(
  ":packages:base",
  // ":packages:cli",
  ":packages:cli-bridge",
  ":packages:core",
  ":packages:engine",
  ":packages:graalvm",
  ":packages:graalvm-ts",
  ":packages:graalvm-jvm",
  ":packages:graalvm-llvm",
  ":packages:graalvm-wasm",
  ":packages:graalvm-py",
  ":packages:graalvm-rb",
  ":packages:graalvm-kt",
  ":packages:graalvm-java",
  ":packages:http",
  ":packages:proto:proto-core",
  ":packages:proto:proto-test",
  ":packages:proto:proto-capnp",
  ":packages:proto:proto-kotlinx",
  ":packages:proto:proto-protobuf",
  ":packages:platform",
  ":packages:bom",
  ":packages:runtime",
  ":packages:server",
  ":packages:ssr",
  ":packages:test",
  ":packages:sqlite",
  // @TODO: Broken.
  // ":packages:tcnative",
  ":packages:terminal",
  ":packages:tooling",
  ":packages:transport:transport-common",
  ":packages:transport:transport-unix",
  ":packages:transport:transport-epoll",
  ":packages:transport:transport-kqueue",
  ":packages:transport:transport-uring",
  ":tools:umbrella",
  ":tools:reports",
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

val cachePush: String? by settings
val isCI: Boolean = System.getenv("CI") != "true"
val enableRemoteCache: String? by settings

buildless {
  remoteCache {
    enabled = enableRemoteCache == "true"

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
