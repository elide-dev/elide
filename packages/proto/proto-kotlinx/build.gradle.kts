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

@file:OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)

@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions


plugins {
  `maven-publish`
  distribution
  signing
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("dev.elide.build.multiplatform.jvm")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

configurations {
  // `modelInternalJvm` is the dependency used internally by other Elide packages to access the protocol model. at
  // present, the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternalJvm") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["jvmRuntimeClasspath"])
  }
}

kotlin {
  jvm {
    withJava()
  }
  js {
    browser()
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
    d8()
    nodejs()
    browser()
  }

  sourceSets {
    /**
     * Variant: KotlinX
     */
    val jvmMain by getting {
      dependencies {
        // API
        api(libs.kotlinx.datetime)
        api(projects.packages.proto.protoCore)
        api(projects.packages.core)
        implementation(libs.kotlinx.serialization.core.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)

        // Implementation
        implementation(kotlin("stdlib"))
        implementation(kotlin("stdlib-jdk8"))
        runtimeOnly(kotlin("reflect"))
      }
    }
    val jvmTest by getting {
      dependencies {
        // Testing
        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(projects.packages.test)
        implementation(project(":packages:proto:proto-core", configuration = "testBase"))
      }
    }
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        apiVersion = Elide.kotlinLanguage
        languageVersion = Elide.kotlinLanguage
        allWarningsAsErrors = true

        if (this is KotlinJvmOptions) {
          jvmTarget = javaLanguageTarget
          javaParameters = true
        }
      }
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = true
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  jvmTest {
    useJUnitPlatform()
  }

  artifacts {
    archives(jvmJar)
    add("modelInternalJvm", jvmJar)
  }
}

val sourcesJar by tasks.getting(org.gradle.jvm.tasks.Jar::class)

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
    artifactId = artifactId.replace("proto-kotlinx", "elide-proto-kotlinx")

    pom {
      name = "Elide Protocol: KotlinX"
      description = "Elide protocol implementation for KotlinX Serialization"
      url = "https://elide.dev"
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
