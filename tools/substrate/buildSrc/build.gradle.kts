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

@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

val buildDocs by properties
val enableAtomicfu = project.properties["elide.atomicFu"] == "true"
val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

repositories {
  maven("https://maven.pkg.st")
  maven("https://gradle.pkg.st")
}

val kotlinVersion by properties

dependencies {
  implementation(gradleApi())
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.sigstore)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.ksp)
  implementation(kotlin("allopen"))
  implementation(kotlin("noarg"))
  implementation(kotlin("serialization"))
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  if (enableAtomicfu) {
    implementation(libs.plugin.kotlinx.atomicfu)
  }
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

afterEvaluate {
  tasks {
    compileKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }

    compileTestKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }
  }
}

apply(from = "../../../gradle/loadProps.gradle.kts")
