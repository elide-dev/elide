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


import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlinx.atomicfu.plugin.gradle.AtomicFUPluginExtension
import elide.internal.conventions.kotlin.*
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("multiplatform")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  
  alias(libs.plugins.micronaut.library)
  alias(libs.plugins.micronaut.graalvm)

  id(libs.plugins.ksp.get().pluginId)
  id("elide.internal.conventions")
  idea
}

apply(plugin = "kotlinx-atomicfu")

the<AtomicFUPluginExtension>().apply {
  dependenciesVersion = null
  transformJvm = true
  transformJs = true
  jvmVariant = "VH"
}

idea {
  module {
    languageLevel = IdeaLanguageLevel("21")
    targetBytecodeVersion = JavaVersion.VERSION_21
  }
}

kotlin {
  jvm {
    withJava()
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_2_0
    languageVersion = KotlinVersion.KOTLIN_2_0
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        if (this is KotlinJvmCompilerOptions) {
          jvmTarget = JvmTarget.JVM_21
          javaParameters = true
        }
      }
    }
  }
}

elide {
  publishing {
    id = "server"
    name = "Elide for Serverless"
    description = "Serverless dispatch interfaces for Elide applications."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.Embedded
    explicitApi = true
  }

  java {
    configureModularity = false
  }
}

group = "dev.elide"
version = rootProject.version as String

micronaut {
  version = libs.versions.micronaut.lib.get()

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.serverless",
      "elide.serverless.*",
      "elide.serverless.annotations",
      "elide.serverless.annotations.*",
    ))
  }
}

dependencies {
  common {
    api(projects.packages.core)
    api(projects.packages.base)
    api(projects.packages.http)
    api(projects.packages.ssr)

    // Okio
    api(libs.okio)

    // KotlinX
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.protobuf)
    api(libs.kotlinx.io)
    implementation(libs.kotlinx.atomicfu)
  }

  // Testing
  commonTest {
    api(projects.packages.test)
    api(kotlin("test"))
  }

  jvm {
    api(mn.micronaut.http)
  }

  jvmTest {
    api(libs.truth)
    api(libs.truth.java8)
    api(libs.testing.faker)
    api(libs.testing.mockito.junit)
    api(libs.testing.hamcrest)
    api(kotlin("test-junit5"))
  }
}
