
val protobufVersion = project.properties["versions.protobuf"] as String
val grpcVersion = project.properties["versions.grpc"] as String
val grpcKotlinVersion = project.properties["versions.grpcKotlin"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String

plugins {
  java
  jacoco
  idea
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  id("io.micronaut.library")
}

micronaut {
  version.set(Versions.micronaut)
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

dependencies {
  implementation(project(":base"))
  implementation(project(":server"))
  implementation("io.grpc:grpc-core:$grpcVersion")
  implementation("io.grpc:grpc-api:$grpcVersion")
  implementation("io.grpc:grpc-auth:$grpcVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
  implementation("io.grpc:grpc-services:$grpcVersion")
  implementation("io.grpc:grpc-netty:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:$kotlinxSerializationVersion")
}
