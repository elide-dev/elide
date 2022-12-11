@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.*
import Java9Modularity.configureJava9ModuleInfo

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
  `java`
  alias(libs.plugins.protobuf)
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
    ofSourceSet("jvmMain").forEach {
      it.plugins {
        id("grpc")
        id("grpckt")
      }
      it.builtins {
        id("kotlin")
      }
    }
    ofSourceSet("jvmTest").forEach {
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
  js(IR) {
    nodejs {}
    browser {}
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":packages:base"))
        api(project(":packages:core"))
        api(project(":packages:model"))
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
        implementation(project(":packages:base"))
        implementation(project(":packages:server"))

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
        implementation(libs.micronaut.http)
        implementation(libs.micronaut.context)
        implementation(libs.micronaut.inject)
        implementation(libs.micronaut.inject.java)
        implementation(libs.micronaut.management)
        implementation(libs.micronaut.grpc.runtime)
        implementation(libs.micronaut.grpc.client.runtime)
        implementation(libs.micronaut.grpc.server.runtime)

        // Serialization
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.core.jvm)
        implementation(libs.kotlinx.serialization.json.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)

        // Coroutines
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.jdk8)
        implementation(libs.kotlinx.coroutines.guava)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("test-junit5"))

        // Testing
        implementation(project(":packages:test"))
        implementation(kotlin("test-junit5"))
        implementation(libs.micronaut.test.junit5)
        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(project(":packages:base"))
        implementation(project(":packages:frontend"))
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
        implementation(project(":packages:test"))
      }
    }
  }
}

configureJava9ModuleInfo(
  multiRelease = true,
)

tasks {
  withType(Copy::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
  withType(Jar::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
