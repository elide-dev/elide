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

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.allopen.gradle.*

plugins {
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")

  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

val javaLanguageVersion = project.properties["versions.java.language"] as String

sourceSets.all {
  java.setSrcDirs(listOf("jmh/src"))
  resources.setSrcDirs(listOf("jmh/resources"))
}

dependencies {
  kapt(mn.micronaut.inject.java)
  implementation(libs.kotlinx.benchmark.runtime)
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.test)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)
  implementation(mn.micronaut.aop)
  implementation(mn.micronaut.context)
  implementation(mn.micronaut.inject.java.test)
  implementation(mn.micronaut.http.client)
  implementation(mn.micronaut.http.server)
  implementation(mn.micronaut.http.validation)
  implementation(mn.micronaut.validation)
  implementation(mn.micronaut.runtime)
  implementation(libs.micronaut.router)
  implementation(libs.kotlinx.wrappers.css)
  implementation(libs.reactor.core)
  implementation(libs.reactor.netty)
  implementation(libs.reactor.netty.core)
  implementation(libs.reactor.netty.http)
  runtimeOnly(libs.logback)

  implementation(projects.packages.base)
  implementation(projects.packages.server)
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
      jmhVersion = libs.versions.jmh.lib.get()
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
    jvmTarget = javaLanguageVersion
    javaParameters = true
    incremental = true
  }
}
