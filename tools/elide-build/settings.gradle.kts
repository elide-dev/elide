/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "elide-internal-plugin"

pluginManagement {
  repositories {
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.jetbrains.reflekt")
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
    }
    maven {
      name = "maven-central-explicit"
      url = uri("https://repo1.maven.org/maven2/")
    }
    maven {
      name = "kotlin-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
      content {
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

plugins {
  id("com.gradle.enterprise") version("3.16.2")
  id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

dependencyResolutionManagement {
  repositories {
    maven {
      name = "elide-snapshots"
      url = uri("https://maven.elide.dev")
      content {
        includeGroup("dev.elide")
        includeGroup("org.capnproto")
        includeGroup("com.google.devtools.ksp")
        includeGroup("org.jetbrains.reflekt")
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
        includeGroup("com.google.devtools.ksp")
      }
    }
    maven {
      name = "maven-central-explicit"
      url = uri("https://repo1.maven.org/maven2/")
    }
    maven {
      name = "kotlin-dev"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
      content {
        includeGroupByRegex("org\\.jetbrains.*")
      }
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }

  versionCatalogs {
    create("libs") {
      from(files("../../gradle/elide.versions.toml"))
    }
  }
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

buildCache {
  local.isEnabled = true
}
