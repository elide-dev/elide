/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

plugins {
  `maven-publish`
  distribution
  signing
  java
  `java-test-fixtures`
  `embedded-kotlin`

  id("dev.elide.build")
  id("dev.elide.build.jvm")
  id("dev.elide.build.kotlin")
  id("dev.elide.build.substrate")
}

group = "dev.elide.tools"
version = rootProject.version as String

kotlin {
  explicitApi()

  sourceSets.all {
    languageSettings.apiVersion = ElideSubstrate.KOTLIN_VERSION
    languageSettings.languageVersion = ElideSubstrate.KOTLIN_VERSION
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
}

val buildDocs = (project.properties["buildDocs"] as? String ?: "true") == "true"
val test: Configuration by configurations.creating

dependencies {
  api(libs.google.auto.service.annotations)
  implementation(libs.kotlin.compiler.embedded)

  testApi(kotlin("test"))
  testApi(libs.junit.jupiter.api)
  testApi(libs.junit.jupiter.params)
  testApi(libs.kotlin.compiler.testing)
  testApi(libs.truth)
  testApi(libs.truth.proto)
  testApi(libs.truth.java8)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.kotlin.compiler.embedded)
}

val testArchive by tasks.registering(Jar::class) {
  archiveClassifier = "tests"
  from(sourceSets["test"].output)
}

artifacts {
  add("test", testArchive)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      group = "dev.elide.tools"
      artifactId = "compiler-util"
      version = rootProject.version as String
      from(components["kotlin"])
      artifact(tasks["testArchive"])

      pom {
        name = "Elide Substrate: Compiler Utilities"
        url = "https://github.com/elide-dev/v3"
        description = "Provides utilities for Elide Kotlin Compiler plugins."

        licenses {
          license {
            name = "MIT License"
            url = "https://github.com/elide-dev/v3/blob/v3/LICENSE"
          }
        }
        developers {
          developer {
            id = "sgammon"
            name = "Sam Gammon"
            email = "samuel.gammon@gmail.com"
          }
        }
        scm {
          url = "https://github.com/elide-dev/v3"
        }
      }
    }
  }
}
