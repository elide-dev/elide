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
  "unused_variable",
  "DSL_SCOPE_VIOLATION",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import Java9Modularity.configure as configureJava9ModuleInfo

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()

  js {
    browser()
    nodejs()
    generateTypeScriptDefinitions()

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
  }
  wasm {
    browser()
    nodejs()
    d8()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(projects.packages.base)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(projects.packages.test)
      }
    }
    val jvmMain by getting {
      dependencies {
        api(libs.micronaut.http)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(projects.packages.test)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        runtimeOnly(libs.junit.jupiter.engine)
      }
    }
    val jsMain by getting {
      dependencies {
        // KT-57235: fix for atomicfu-runtime error
        api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
      }
    }
    val jsTest by getting
  }
}

configureJava9ModuleInfo(project)

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier = "javadoc"
    from(dokkaHtml.outputDirectory)
  }
  javadocJar
} else null

publishing {
  publications.withType<MavenPublication> {
    if (buildDocs) {
      artifact(javadocJar)
    }
    artifactId = artifactId.replace("ssr", "elide-ssr")

    pom {
      name = "Elide SSR"
      url = "https://elide.dev"
      description = "Package for server-side rendering (SSR) capabilities with the Elide Framework."

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
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
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}

afterEvaluate {
  tasks.named("compileTestDevelopmentExecutableKotlinJs") {
    enabled = false
  }
  tasks.named("compileTestDevelopmentExecutableKotlinWasm") {
    enabled = false
  }
}
