@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()

  js {
    browser()
    nodejs()
    binaries.executable()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(project(":packages:base"))
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(project(":packages:test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        api(libs.micronaut.http)
        compileOnly(libs.graalvm.sdk)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(project(":packages:test"))
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.protobuf)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.datetime)
        runtimeOnly(libs.junit.jupiter.engine)
      }
    }
    val jsMain by getting
    val jsTest by getting
  }
}
