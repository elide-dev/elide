@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.*

plugins {
  alias(libs.plugins.protobuf)
  id("io.micronaut.library")
  id("dev.elide.build.jvm.kapt")
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String


micronaut {
  version.set(libs.versions.micronaut.lib.get())
  processing {
    incremental.set(true)
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
    ofSourceSet("main").forEach {
      it.plugins {
        id("grpc")
        id("grpckt")
      }
      it.builtins {
        id("kotlin")
      }
    }
    ofSourceSet("test").forEach {
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
  // Core platform versions.
  api(platform(project(":packages:bom")))

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

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.micronaut.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.logback)
}

tasks.jacocoTestReport {
  reports {
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.dokkaHtml.configure {
  moduleName.set("rpc-jvm")
}
