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

import ElidePackages.elidePackage
import com.google.protobuf.gradle.id

plugins {
  java
  kotlin("kapt")
  alias(libs.plugins.protobuf)

  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
  id("dev.elide.build.publishable")
}

group = "dev.elide"
version = rootProject.version as String

val ideaActive = properties["idea.active"] == "true"

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.java.get()}"
    }
    id("grpckt") {
      artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar"
    }
  }
  generateProtoTasks {
    all().forEach {
      it.addIncludeDir(files("$projectDir/src/jvmTest/proto"))
      it.addSourceDirs(files("$projectDir/src/jvmTest/proto"))

      it.plugins {
        id("grpc")
        id("grpckt")
      }
      it.builtins {
        id("kotlin")
      }
    }
  }
}

kotlin {
  explicitApi()

  jvm {
    withJava()
  }
  js {
    nodejs {}
    browser {}
    generateTypeScriptDefinitions()

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.packages.base)
        api(projects.packages.core)
        api(projects.packages.model)
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(projects.packages.base)
        implementation(projects.packages.server)
        configurations["kapt"].dependencies.add(mn.micronaut.inject.java.asProvider().get())

        // Protobuf
        implementation(libs.protobuf.java)
        implementation(libs.protobuf.util)
        implementation(libs.protobuf.kotlin)

        // gRPC
        implementation(libs.grpc.api)
        implementation(libs.grpc.auth)
        implementation(libs.grpc.core)
        implementation(libs.grpc.stub)
        implementation(libs.grpc.services)
        implementation(libs.grpc.netty)
        implementation(libs.grpc.protobuf)
        implementation(libs.grpc.kotlin.stub)

        // Micronaut
        implementation(mn.micronaut.http)
        implementation(mn.micronaut.context)
        implementation(mn.micronaut.inject)
        implementation(mn.micronaut.inject.java)
        implementation(mn.micronaut.management)
        implementation(mn.micronaut.grpc.runtime)
        implementation(mn.micronaut.grpc.client.runtime)
        implementation(mn.micronaut.grpc.server.runtime)

        // Serialization
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.core.jvm)
        implementation(libs.kotlinx.serialization.json.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)

        // Coroutines
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.guava)
      }
    }
    val jvmTest by getting {
      kotlin.srcDirs(
        layout.projectDirectory.dir("src/jvmTest/kotlin"),
        layout.buildDirectory.dir("generated/source/proto/test/grpckt"),
        layout.buildDirectory.dir("generated/source/proto/test/kotlin"),
      )

      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("test-junit5"))
        configurations["kaptTest"].dependencies.add(mn.micronaut.inject.java.asProvider().get())

        // Testing
        implementation(projects.packages.test)
        implementation(kotlin("test-junit5"))
        implementation(mn.micronaut.test.junit5)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(projects.packages.base)
        implementation(projects.packages.frontend)
        implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
        implementation(npm("google-protobuf", libs.versions.npm.google.protobuf.get()))
        implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(kotlin("test"))
        implementation(projects.packages.test)
      }
    }
  }
}

tasks {
  withType(Copy::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  named("compileTestKotlinJvm").configure {
    dependsOn("generateProto", "generateTestProto")
  }
}

afterEvaluate {
  listOf(
    "kaptGenerateStubsKotlinJvm",
    "kaptGenerateStubsTestKotlinJvm",
    "runKtlintCheckOverJvmTestSourceSet",
  ).forEach {
    tasks.named(it) {
      dependsOn(tasks.generateProto, tasks.generateTestProto)
    }
  }
}

elidePackage(
  id = "rpc",
  name = "Elide RPC",
  description = "Cross-platform RPC dispatch and definition tools and runtime utilities.",
) {
  java9Modularity = false
}
