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

@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  distribution
  signing
  idea
  java
  `jvm-toolchains`

  id("java-gradle-plugin")
  `kotlin-dsl`
}

group = "dev.elide.tools"
version = rootProject.version as String

gradlePlugin {
  plugins {
    create("elideToolchainManager") {
      id = "elide.toolchains.jvm"
      implementationClass = "elide.toolchain.jvm.JvmToolchainResolverPlugin"
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
    freeCompilerArgs = listOf(
      "-Xcontext-receivers",
      "-Xskip-prerelease-check",
      "-Xsuppress-version-warnings",
      "-Xjvm-default=all",
      "-Xjsr305=strict",
    )
  }
}

fun MutableList<String>.addIfNotPresent(arg: String) {
  if (!contains(arg)) add(arg)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = "2.0"
    languageVersion = "2.0"
    freeCompilerArgs = freeCompilerArgs.toMutableList().apply {
      addIfNotPresent("-Xcontext-receivers")
      addIfNotPresent("-Xskip-prerelease-check")
      addIfNotPresent("-Xsuppress-version-warnings")
      addIfNotPresent("-Xjvm-default=all")
      addIfNotPresent("-Xjsr305=strict")
    }
  }
}

dependencies {
  implementation(gradleApi())
}
