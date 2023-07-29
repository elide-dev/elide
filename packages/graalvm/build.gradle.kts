@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import Java9Modularity.configure as configureJava9ModuleInfo
import kotlinx.benchmark.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")

  kotlin("plugin.allopen")
  id("dev.elide.build.native.lib")
  alias(libs.plugins.jmh)
  alias(libs.plugins.kotlinx.plugin.benchmark)
}

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

group = "dev.elide"
version = rootProject.version as String


kotlin {
  explicitApi()
}

sourceSets {
  val benchmarks by creating {
    kotlin.srcDirs(
      "$projectDir/src/benchmarks/kotlin",
      "$projectDir/src/main/kotlin",
    )
  }
}

graalvmNative {
  agent {
    enabled.set(false)
  }
}

benchmark {
  configurations {
    named("main") {
      warmups = 10
      iterations = 5
    }
  }
  targets {
    register("benchmarks") {
      this as JvmBenchmarkTarget
      jmhVersion = "1.36"
    }
  }
}

micronaut {
  enableNativeImage(true)
  version.set(libs.versions.micronaut.lib.get())
  processing {
    incremental.set(true)
    annotations.addAll(listOf(
      "elide.runtime.*",
      "elide.runtime.gvm.*",
      "elide.runtime.gvm.internals.*",
      "elide.runtime.gvm.intrinsics.*",
    ))
  }
}

configurations["benchmarksImplementation"].extendsFrom(
  configurations.implementation.get(),
  configurations.testImplementation.get(),

)
configurations["benchmarksRuntimeOnly"].extendsFrom(
  configurations.runtimeOnly.get(),
  configurations.testRuntimeOnly.get()
)

dependencies {
  // API Deps
  api(libs.jakarta.inject)

  // Modules
  api(project(":packages:base"))
  api(project(":packages:core"))
  implementation(project(":packages:ssr"))

  // Kotlin / KotlinX
  implementation(kotlin("stdlib"))
  implementation(kotlin("reflect"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  runtimeOnly(libs.kotlinx.coroutines.reactor)

  // General
  implementation(libs.jimfs)
  implementation(libs.lmax.disruptor.core)

  // Compression
  implementation(libs.commons.compress)

  // Micronaut
  runtimeOnly(libs.micronaut.graal)
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)

  // Reactor Netty
  implementation(libs.reactor.netty)

  // SQLite
  implementation(libs.sqlite)

  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-protobuf"))
  implementation(project(":packages:proto:proto-kotlinx"))
  implementation(project(":packages:proto:proto-flatbuffers"))

  compileOnly(libs.graalvm.sdk)
  compileOnly(libs.graalvm.truffle.api)

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.micronaut.test.junit5)
  testCompileOnly(libs.graalvm.sdk)
  testRuntimeOnly(libs.junit.jupiter.engine)
}

configureJava9ModuleInfo(project)

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("graalvm", "elide-graalvm")

    pom {
      name.set("Elide for GraalVM")
      url.set("https://elide.dev")
      description.set(
        "Integration package with GraalVM and GraalJS."
      )

      licenses {
        license {
          name.set("MIT License")
          url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
        }
      }
      developers {
        developer {
          id.set("sgammon")
          name.set("Sam Gammon")
          email.set("samuel.gammon@gmail.com")
        }
      }
      scm {
        url.set("https://github.com/elide-dev/elide")
      }
    }
  }
}

val compileKotlin: KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileKotlin.destinationDirectory.set(compileJava.destinationDirectory)

tasks.jar {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Unused dependencies:
//  implementation(libs.lmax.disruptor.proxy)
//  implementation(libs.brotli)
//  implementation(libs.micronaut.inject.java)
//  implementation(libs.micronaut.cache.core)
//  implementation(libs.micronaut.cache.caffeine)
//  implementation(libs.lz4)
//  implementation(libs.flatbuffers.java.core)
//  implementation(libs.kotlinx.coroutines.slf4j)
