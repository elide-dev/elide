@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

plugins {
  java
  jacoco
  idea
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  id("io.micronaut.library")
  id("com.adarshr.test-logger")
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.javaLanguage))
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
    vendor.set(JvmVendorSpec.GRAAL_VM)
    if (project.hasProperty("elide.graalvm.variant")) {
      val variant = project.property("elide.graalvm.variant") as String
      if (variant != "COMMUNITY") {
        vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
          "ENTERPRISE" -> "GraalVM Enterprise"
          else -> "GraalVM Community"
        }))
      }
    }
    if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {

    }
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

micronaut {
  version.set(Versions.micronaut)
}

// add to graalvm flags:
// -Dpolyglot.image-build-time.PreinitializeContexts=js

dependencies {
  // API Deps
  api("jakarta.inject:jakarta.inject-api:2.0.1")

  // Modules
  implementation(project(":base"))
  implementation(project(":server"))

  // KotlinX
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:${Versions.coroutinesVersion}")

  // Google
  implementation("com.google.guava:guava:${Versions.guava}")

  // Micronaut
  implementation("io.micronaut:micronaut-http:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-context:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject-java:${Versions.micronaut}")

  // GraalVM SDK
  implementation("org.graalvm.sdk:graal-sdk:${Versions.graalvm}")
}
