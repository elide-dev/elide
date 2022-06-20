@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

plugins {
  java
  jacoco
  idea
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("io.micronaut.library")
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.javaLanguage))
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
  }
  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "**/generated/**",
          "**/com/**",
          "**/grpc/gateway/**",
          "**/tools/elide/**",
        )
      }
    })
  )
}

micronaut {
  version.set(Versions.micronaut)
}

dependencies {
  // API Deps
  api("jakarta.inject:jakarta.inject-api:2.0.1")
  api("org.slf4j:slf4j-api:${Versions.slf4j}")
  api(platform("io.netty:netty-bom:${Versions.netty}"))
  api(platform("io.grpc:grpc-bom:${Versions.grpc}"))
  api(platform("io.netty:netty-bom:${Versions.netty}"))

  // Modules
  implementation(project(":base"))

  // Kotlin
  implementation(kotlin("reflect"))
  implementation(kotlin("stdlib-jdk8"))
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:${Versions.kotlinSerialization}")

  // Kotlin Wrappers
  implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-${Versions.kotlinWrappers}")

  // gRPC
  implementation("io.grpc:grpc-core")
  implementation("io.grpc:grpc-api")
  implementation("io.grpc:grpc-auth")
  implementation("io.grpc:grpc-stub")
  implementation("io.grpc:grpc-services")
  implementation("io.grpc:grpc-netty")
  implementation("io.grpc:grpc-protobuf")
  implementation("io.grpc:grpc-kotlin-stub:${Versions.grpcKotlin}")

  // Protocol Buffers
  implementation("com.google.protobuf:protobuf-java:${Versions.protobuf}")
  implementation("com.google.protobuf:protobuf-kotlin:${Versions.protobuf}")

  // Micronaut
  implementation("io.micronaut:micronaut-http")
  implementation("io.micronaut.grpc:micronaut-grpc-runtime:${Versions.micronautGrpc}")
  implementation("io.micronaut.grpc:micronaut-grpc-client-runtime:${Versions.micronautGrpc}")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.coroutinesVersion}")
}
