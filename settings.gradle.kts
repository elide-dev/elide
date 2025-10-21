/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

pluginManagement {
  repositories {
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
      content {
        includeGroup("com.google.guava")
        includeGroup("dev.javamodules")
      }
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("com.google.devtools.ksp.gradle.plugin")
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
  id("com.autonomousapps.build-health") version ("2.10.1")
  id("com.gradle.enterprise") version ("3.16.2")
  id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
  id("com.gradle.common-custom-user-data-gradle-plugin") version ("2.1")
  id("io.micronaut.platform.catalog") version (extra.properties["micronautCatalogVersion"] as String)
  id("elide.toolchains.jvm")
}

buildscript {
  repositories {
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("com.google.devtools.ksp")
      }
    }
  }
  dependencies {
    val asm = "9.8"
    classpath("org.ow2.asm:asm:$asm")
    classpath("org.ow2.asm:asm-tree:$asm")
  }
}

// Fix: Force CWD to proper value and store secondary value.
System.setProperty("user.dir", rootProject.projectDir.toString())
System.setProperty("elide.home", rootProject.projectDir.toString())

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
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
        includeGroup("net.java.dev.jna")
        includeGroup("org.jetbrains.reflekt")
        includeGroup("org.pkl-lang")
        includeGroup("dev.karmakrafts")
        includeGroup("dev.karmakrafts.filament")
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
      name = "jpms-modules"
      url = uri("https://jpms.pkg.st/repository")
      content {
        includeGroup("com.google.guava")
        includeGroup("dev.javamodules")
      }
    }
    maven {
      name = "oss-snapshots"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      content {
        includeGroup("dev.elide")
        includeGroup("com.google.devtools.ksp")
        includeGroup("com.google.devtools.ksp.gradle.plugin")
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
      content {
        includeGroup("com.google.devtools.ksp")
        includeGroup("com.google.devtools.ksp.gradle.plugin")
      }
    }
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
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

// Build modules.
include(
  ":packages:base",
  ":packages:bom",
  ":packages:builder",
  ":packages:cli",
  ":packages:core",
  ":packages:engine",
  ":packages:exec",
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
  ":packages:local-ai",
  ":packages:platform",
  ":packages:progress",
  ":packages:runner",
  ":packages:secrets",
  ":packages:server",
  ":packages:sqlite",
  ":packages:ssr",
  ":packages:terminal",
  ":packages:telemetry",
  ":packages:test",
  ":packages:tooling",
  ":tools:reports",
  ":tools:umbrella",
)

val buildEmbedded: String by settings
val buildBenchmarks: String by settings
val enableNativeTransport: String by settings
val buildExperimentalEntrypoint: String by settings
val buildIdePlugins: String by settings

if (buildExperimentalEntrypoint.toBoolean()) {
  include(":packages:entry")
}

if (buildEmbedded.toBoolean()) {
  include(
    ":packages:embedded",
  )
}

if (enableNativeTransport.toBoolean()) {
  include(
    ":packages:tcnative",
    ":packages:transport:transport-common",
    ":packages:transport:transport-epoll",
    ":packages:transport:transport-kqueue",
    ":packages:transport:transport-unix",
    ":packages:transport:transport-uring",
  )
}

if (buildBenchmarks.toBoolean()) {
  include(
    ":tools:benchmarks:bench-graalvm",
  )
}

if (buildIdePlugins.toBoolean()) {
  include(
    ":packages:plugin-idea",
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

buildCache {
  local.isEnabled = true
}

buildless {
  localCache {
    enabled = true
  }
  remoteCache {
    enabled = false
  }
}

//
// -- End Caching Tricks
//

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val elidePluginVersion: String by settings
apply(from = "https://gradle.elide.dev/$elidePluginVersion/elide.gradle.kts")
