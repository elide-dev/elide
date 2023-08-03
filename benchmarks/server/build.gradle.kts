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
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*
import tools.elide.assets.EmbeddedScriptLanguage

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  alias(libs.plugins.elide)
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

val javaLanguageVersion = project.properties["versions.java.language"] as String

elide {
  mode = BuildMode.PRODUCTION

  server {
    assets {
      bundler {
        format(tools.elide.assets.ManifestFormat.BINARY)
        compression {
          minimumSizeBytes(0)
          keepAllVariants()
        }
      }
    }
  }
}

sourceSets.all {
  java.setSrcDirs(listOf("jmh/src"))
  resources.setSrcDirs(listOf("jmh/resources"))
}

dependencies {
  kapt(libs.micronaut.inject.java)
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.test)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(libs.micronaut.aop)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject.java.test)
  implementation(libs.micronaut.http.client)
  implementation(libs.micronaut.http.server)
  implementation(libs.micronaut.http.validation)
  implementation(libs.micronaut.validation)
  implementation(libs.micronaut.runtime)
  implementation(libs.micronaut.router)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.reactor.core)
  implementation(libs.reactor.netty)
  implementation(libs.reactor.netty.core)
  implementation(libs.reactor.netty.http)
  runtimeOnly(libs.logback)

  implementation(libs.elide.base)
  implementation(libs.elide.server)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
  configurations {
    named("main") {
      warmups = 3
      iterations = 3

      if (!project.hasProperty("elide.benchmark")) {
        listOf(
          "core",
          "server",
        ).forEach { module ->
          include("elide.benchmarks.$module.*")
        }
        exclude(
          "elide.benchmarks.server.PageBenchmarkHttp"
        )
      } else {
        include(project.properties["elide.benchmark"] as String)
      }
    }
  }
  targets {
    register("main") {
      this as JvmBenchmarkTarget
      jmhVersion = "1.36"
    }
  }
}

tasks.withType(Jar::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType(Copy::class).configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = "1.9"
    languageVersion = "1.9"
    jvmTarget = javaLanguageVersion
    javaParameters = true
    incremental = true
  }
}
