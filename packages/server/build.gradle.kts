@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import org.apache.tools.ant.taskdefs.condition.Os


plugins {
  id("io.micronaut.library")
  id("io.micronaut.graalvm")
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String
val encloseSdk = false

kotlin {
  explicitApi()
}

micronaut {
  version = libs.versions.micronaut.lib.get()

  processing {
    incremental = true
    annotations.addAll(listOf(
      "elide.server",
      "elide.server.*",
      "elide.server.annotations",
      "elide.server.annotations.*",
    ))
  }
}

dependencies {
  // BOMs
  api(platform(libs.netty.bom))

  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)

  // Modules
  api(projects.packages.base)
  api(projects.packages.core)
  api(projects.packages.model)
  api(projects.packages.ssr)
  api(projects.packages.graalvm)
  api(libs.reactor.core)

  // KSP
  kapt(libs.micronaut.inject)
  kapt(libs.micronaut.inject.java)

  implementation(projects.packages.proto.protoCore)
  implementation(projects.packages.proto.protoProtobuf)
  implementation(projects.packages.proto.protoKotlinx)

  // Crypto
  implementation(libs.bouncycastle)
  implementation(libs.bouncycastle.pkix)
  implementation(libs.conscrypt)
  implementation(libs.tink)

  // Reactive Java
  implementation(libs.reactor.core)
  implementation(libs.reactor.netty.core)
  implementation(libs.reactor.netty.http)

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.css)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // Brotli
  implementation(libs.brotli)
  implementation(libs.brotli.native.osx)
  implementation(libs.brotli.native.linux)
  implementation(libs.brotli.native.windows)

  // Micronaut
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.http.server)
  implementation(libs.micronaut.http.server.netty)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.protobuf)
  implementation(libs.micronaut.management)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactor)
  implementation(libs.kotlinx.coroutines.reactive)

  // General
  implementation(libs.reactivestreams)
  implementation(libs.google.common.html.types.types)

  if (encloseSdk) {
    compileOnly(libs.graalvm.sdk)
  }

  // Netty: Native
  implementation(libs.netty.tcnative)

  val arch = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64", "aarch_64" -> "aarch_64"
    else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
  }
  when {
    Os.isFamily(Os.FAMILY_WINDOWS) -> {
      implementation(libs.netty.tcnative.boringssl.static)
    }

    Os.isFamily(Os.FAMILY_UNIX) -> {
      when {
        Os.isFamily(Os.FAMILY_MAC) -> {
          implementation(libs.netty.transport.native.kqueue)
          implementation(libs.netty.transport.native.kqueue)
          implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
          implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
          implementation(libs.netty.resolver.dns.native.macos)
        }

        else -> {
          implementation(libs.netty.transport.native.epoll)
          implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-$arch") })
          implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-$arch") })
        }
      }
    }

    else -> {}
  }

  // Testing
  kaptTest(libs.micronaut.inject)
  kaptTest(libs.micronaut.inject.java)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(libs.micronaut.test.junit5)
  testImplementation(kotlin("test"))
  testImplementation(projects.packages.test)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
  dokkaSourceSets {
    configureEach {
      includes.from("module.md")
//      samples.from("samples/basic.kt")
    }
  }
}

graalvmNative {
  testSupport = true

  metadataRepository {
    enabled = true
    version = GraalVMVersions.graalvmMetadata
  }

  agent {
    enabled = false
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing {
  publications.withType<MavenPublication> {
    artifactId = artifactId.replace("server", "elide-server")

    pom {
      name = "Elide for Servers"
      url = "https://elide.dev"
      description = "Server-side tools, framework, and runtime, based on GraalVM and Micronaut"

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}
