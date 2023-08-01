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

val kotlinVersion = "1.9.0"

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  `embedded-kotlin`
}

val buildDocs by properties
val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

dependencies {
  implementation(gradleApi())
  api(libs.elide.tools.conventions)
  implementation(libs.elide.kotlin.plugin.redakt)
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.micronaut)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.shadow)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.plugin.ksp)
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(embeddedKotlin("allopen"))
  implementation(embeddedKotlin("noarg"))
  implementation(embeddedKotlin("lombok"))
  implementation(embeddedKotlin("serialization"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-sam-with-receiver")
  }
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

afterEvaluate {
  tasks {
    compileKotlin.configure {
      kotlinOptions {
        apiVersion = "1.9"
        languageVersion = "1.9"
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }

    compileTestKotlin.configure {
      kotlinOptions {
        apiVersion = "1.9"
        languageVersion = "1.9"
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }
  }
}

apply(from = "../gradle/loadProps.gradle.kts")
