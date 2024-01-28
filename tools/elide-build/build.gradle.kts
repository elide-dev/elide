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

@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  `kotlin-dsl`
}

group = "dev.elide.tools"
version = rootProject.version as String

gradlePlugin {
  plugins {
    create("elideInternalBuild") {
      id = "elide.internal.conventions"
      implementationClass = "elide.internal.conventions.ElideConventionPlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  explicitApi()
  
  compilerOptions {
    jvmTarget = JVM_21
    javaParameters = true
    allWarningsAsErrors = false

    apiVersion = KOTLIN_2_0
    languageVersion = KOTLIN_2_0
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    languageVersion = "2.0"
  }
}

dependencies {
  implementation(gradleApi())

  // included plugins
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.kotlin)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.sigstore)
  implementation(libs.plugin.redacted) // @TODO(sgammon): broken on kotlin 2.0
  implementation(libs.plugin.ksp)

  // embedded Kotlin plugins
  implementation(embeddedKotlin("allopen"))
  implementation(embeddedKotlin("noarg"))
  implementation(embeddedKotlin("serialization"))
}

configurations.all {
  resolutionStrategy {
    // fail eagerly on version conflict (includes transitive dependencies)
    failOnVersionConflict()

    // prefer modules that are part of this build
    preferProjectModules()
  }
}
