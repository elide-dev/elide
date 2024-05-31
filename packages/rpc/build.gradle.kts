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

import com.google.protobuf.gradle.id
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import elide.internal.conventions.kotlin.*

plugins {
  java
  kotlin("multiplatform")
  alias(libs.plugins.protobuf)

  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "rpc"
    name = "Elide RPC"
    description = "Cross-platform RPC dispatch and definition tools and runtime utilities."
  }

  kotlin {
    target = KotlinTarget.All
    explicitApi = true
    ksp = true
  }

  java {
    configureModularity = false
    includeSources = false
  }

  checks {
    pmd = false
    checkstyle = false
    javaFormat = false
  }
}

spotless {
  java {
    targetExclude("**/jvmTest/**")
  }
}

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

dependencies {
  common {
    api(projects.packages.base)
    api(projects.packages.core)
    api(projects.packages.model)
  }

  commonTest {
    implementation(projects.packages.test)
  }

  jvm {
    implementation(kotlin("stdlib-jdk8"))
    implementation(projects.packages.server)

    // Protobuf
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.util)
    implementation(libs.protobuf.kotlin)

    // gRPC
    implementation(libs.grpc.api)
    implementation(libs.grpc.auth)
    implementation(libs.grpc.core)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.inprocess)
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

  jvmTest {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("test-junit5"))

    // Testing
    implementation(kotlin("test-junit5"))
    implementation(mn.micronaut.test.junit5)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.jupiter.engine)
    runtimeOnly(libs.logback)
  }

  js {
    implementation(projects.packages.frontend)
    implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
    implementation(npm("google-protobuf", libs.versions.npm.google.protobuf.get()))
    implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

    implementation(libs.kotlinx.coroutines.core.js)
    implementation(libs.kotlinx.serialization.json.js)
    implementation(libs.kotlinx.serialization.protobuf.js)
  }
}

val jvmTest: KotlinSourceSet by kotlin.sourceSets.getting {
  kotlin.srcDirs(
    layout.projectDirectory.dir("src/jvmTest/kotlin"),
    layout.buildDirectory.dir("generated/source/proto/test/grpckt"),
    layout.buildDirectory.dir("generated/source/proto/test/kotlin"),
  )
}

tasks.named("compileTestKotlinJvm").configure {
  dependsOn("generateProto", "generateTestProto")
}

if (pluginManager.hasPlugin("org.jetbrains.kotlin.kapt")) afterEvaluate {
  listOf(
    "kaptGenerateStubsKotlinJvm",
    "kaptGenerateStubsTestKotlinJvm",
  ).forEach {
    tasks.named(it) {
      dependsOn("generateProto", "generateTestProto")
    }
  }
}

tasks.withType(JavaCompile::class.java) {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    listOf(
      "-nowarn",
      "-XDenableSunApiLintControl",
      "-Xlint:-deprecation",
    )
  })
}

tasks.named("compileKotlinJs", Kotlin2JsCompile::class.java) {
  compilerOptions {
    freeCompilerArgs.add("-nowarn")
  }
}

tasks {
  withType(Copy::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
