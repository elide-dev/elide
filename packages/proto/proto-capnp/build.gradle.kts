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
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import ElidePackages.elidePackage

plugins {
  `maven-publish`
  distribution
  signing

  id("dev.elide.build.kotlin")
  id("dev.elide.build.jvm17")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  val main by getting
  val test by getting
}

configurations {
  // `capnpInternal` uses the Cap'N'Proto implementation only, rather than the full cruft of Protocol Buffers non-lite.
  create("capnpInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
  }

  create("capnproto") {
    isCanBeResolved = true
    isCanBeConsumed = false
  }
}

val capnproto: Configuration by configurations.getting

java {
  withJavadocJar()
}

kotlin {
  target.compilations.all {
    kotlinOptions {
      jvmTarget = javaLanguageTarget
      javaParameters = true
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      allWarningsAsErrors = true
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(listOf(
        // do not warn for generated code
        "-nowarn"
      ))
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = true
    }
  }
}

publishing {
  publications.create<MavenPublication>("maven") {
    from(components["kotlin"])
  }
}

elidePackage(
  id = "proto-capnp",
  name = "Elide Protocol: Cap'n'Proto",
  description = "Elide protocol implementation for Cap'n'Proto.",
) {
  java9Modularity = false
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks.test {
  useJUnitPlatform()
}

/**
 * Variant: Cap'N'Proto
 */
//  val compileCapnProtos by creating(FlatBuffers::class) {
//    description = "Generate Flatbuffers code for Kotlin/JVM"
//    inputDir = file("${rootProject.projectDir}/proto")
//    outputDir = file("$projectDir/src/main/flat")
//  }

val sourcesJar by tasks.registering(Jar::class) {
  dependsOn(JavaPlugin.CLASSES_TASK_NAME)
  archiveClassifier.set("sources")
  from(sourceSets["main"].allSource)
}

val javadocJar by tasks.getting

artifacts {
  add("capnpInternal", tasks.jar)
  archives(sourcesJar)
  archives(javadocJar)
}

dependencies {
  // Common
  api(libs.kotlinx.datetime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(projects.packages.core)
  implementation(projects.packages.base)
  testImplementation(projects.packages.test)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)

  // Variant: Cap'n'Proto
  api(projects.packages.proto.protoCore)
  api(libs.capnproto.runtime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))

  capnproto(libs.capnproto.compiler)
}

afterEvaluate {
  tasks.named("runKtlintCheckOverMainSourceSet").configure {
    enabled = false
  }
}
