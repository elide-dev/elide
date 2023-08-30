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
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto

plugins {
  `maven-publish`
  distribution
  signing

  alias(libs.plugins.protobuf)

  id("dev.elide.build.kotlin")
  id("dev.elide.build.jvm17")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  /**
   * Variant: Protocol Buffers
   */
  val main by getting {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
  val test by getting
}

configurations {
  // `modelInternal` is the dependency used internally by other Elide packages to access the protocol model. at present,
  // the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
  }
}

kotlin {
  target.compilations.all {
    kotlinOptions {
      jvmTarget = javaLanguageTarget
      javaParameters = true
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      allWarningsAsErrors = false
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(listOf(
        // do not warn for generated code
        "-nowarn",
        "-Xjavac-arguments=-Xlint:-deprecation",
      ))
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = false
      kotlinOptions.freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(listOf(
        // do not warn for generated code
        "-nowarn",
        "-Xjavac-arguments=-Xlint:-deprecation",
      ))
    }
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.builtins {
        id("kotlin")
      }
    }
  }
}

java {
  withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
  options.compilerArgs.add("-Xlint:-deprecation")
}

tasks {
  test {
    useJUnitPlatform()
    dependsOn(generateTestProto)
  }
  jar {
    dependsOn(generateProto)
  }
}

val sourcesJar by tasks.registering(Jar::class) {
  dependsOn(JavaPlugin.CLASSES_TASK_NAME)
  archiveClassifier = "sources"
  from(sourceSets["main"].allSource)
}

val javadocJar by tasks.getting(Jar::class)

artifacts {
  add("modelInternal", tasks.jar)
  archives(sourcesJar)
  archives(javadocJar)
}

publishing {
  publications.create<MavenPublication>("maven") {
    from(components["kotlin"])
    artifact(sourcesJar)
    artifact(javadocJar)
  }
}

elidePackage(
  id = "proto-protobuf",
  name = "Elide Protocol: Protobuf",
  description = "Elide protocol implementation for Protocol Buffers.",
) {
  java9Modularity = false
}

dependencies {
  // API
  api(libs.kotlinx.datetime)
  api(projects.packages.proto.protoCore)
  api(libs.protobuf.java)
  api(libs.protobuf.util)
  api(libs.protobuf.kotlin)

  // Implementation
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(projects.packages.core)
  implementation(libs.google.common.html.types.proto)
  api(libs.google.common.html.types.types)

  // Compile-only
  compileOnly(libs.google.cloud.nativeImageSupport)

  // Test
  testImplementation(projects.packages.test)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))
}
