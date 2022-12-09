@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }
  js(IR) {
    binaries.executable()
  }

  val publicationsFromMainHost =
    listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"

  publishing {
    publications {
      matching { it.name in publicationsFromMainHost }.all {
        val targetPublication = this@all
        tasks.withType<AbstractPublishToMaven>()
          .matching { it.publication == targetPublication }
          .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
      }
    }
  }

  val hostOs = System.getProperty("os.name")
  val isMingwX64 = hostOs.startsWith("Windows")
  val nativeTarget = when {
    hostOs == "Mac OS X" -> macosX64("native")
    hostOs == "Linux" -> linuxX64("native")
    isMingwX64 -> mingwX64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
  }

  sourceSets.all {
    languageSettings.apply {
      languageVersion = libs.versions.kotlin.language.get()
      apiVersion = libs.versions.kotlin.language.get()
      optIn("kotlin.ExperimentalUnsignedTypes")
      progressiveMode = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(project(":packages:base"))
        implementation(kotlin("test"))
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(project(":packages:server"))
        implementation(libs.jakarta.inject)
        implementation(libs.protobuf.java)
        implementation(libs.protobuf.util)
        implementation(libs.protobuf.kotlin)
        implementation(libs.kotlinx.serialization.json.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.jdk8)
        implementation(libs.kotlinx.coroutines.jdk9)
        implementation(libs.kotlinx.coroutines.guava)
        implementation(libs.grpc.testing)
        implementation(kotlin("test"))
        implementation(kotlin("test-junit5"))
        implementation(libs.jsoup)

        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(libs.truth.proto)

        implementation(libs.micronaut.context)
        implementation(libs.micronaut.runtime)
        implementation(libs.micronaut.test.junit5)
        implementation(libs.micronaut.http)
        implementation(libs.micronaut.http.client)
        implementation(libs.micronaut.http.server)

        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jvmTest by getting
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }
    val jsTest by getting
  }
}
