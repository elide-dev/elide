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
  explicitApi()

  js(IR) {
    nodejs {}
    browser {}
  }

  macosArm64()
  iosArm64()
  iosX64()
  watchosArm32()
  watchosArm64()
  watchosX64()
  tvosArm64()
  tvosX64()
  mingwX64()

  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
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
        api(kotlin("stdlib-common"))
        api(kotlin("test"))
        api(kotlin("test-annotations-common"))
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
        api(kotlin("stdlib-jdk8"))
        api(kotlin("test-junit5"))
        api(libs.jakarta.inject)
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.jdk9)
        api(libs.micronaut.context)
        api(libs.micronaut.runtime)
        api(libs.micronaut.test.junit5)
        api(libs.micronaut.http)
        api(libs.junit.jupiter.api)
        api(libs.junit.jupiter.params)

        implementation(libs.protobuf.java)
        implementation(libs.protobuf.util)
        implementation(libs.protobuf.kotlin)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.guava)
        implementation(libs.grpc.testing)
        implementation(libs.jsoup)

        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(libs.truth.proto)

        implementation(libs.micronaut.http.client)
        implementation(libs.micronaut.http.server)

        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jvmTest by getting
    val jsMain by getting {
      dependencies {
        api(kotlin("stdlib-js"))
        api(kotlin("test"))
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.core.js)
      }
    }
    val jsTest by getting
    val nativeMain by getting {
      dependencies {
        api(kotlin("stdlib"))
      }
    }
    val nativeTest by getting {
      dependencies {
        api(kotlin("stdlib"))
        api(kotlin("test"))
      }
    }

    val mingwX64Main by getting { dependsOn(nativeMain) }
    val macosArm64Main by getting { dependsOn(nativeMain) }
    val iosArm64Main by getting { dependsOn(nativeMain) }
    val iosX64Main by getting { dependsOn(nativeMain) }
    val watchosArm32Main by getting { dependsOn(nativeMain) }
    val watchosArm64Main by getting { dependsOn(nativeMain) }
    val watchosX64Main by getting { dependsOn(nativeMain) }
    val tvosArm64Main by getting { dependsOn(nativeMain) }
    val tvosX64Main by getting { dependsOn(nativeMain) }
  }
}
