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

  jvm {
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }

  js(IR) {
    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
      }
    }
    nodejs {}
    browser {}
  }

  wasm32()
  macosX64()
  macosArm64()
  iosArm32()
  iosArm64()
  iosX64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosX86()
  watchosX64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosX64()
  tvosSimulatorArm64()
  mingwX64()
  linuxX64()
  linuxArm32Hfp()

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
    val nonJvmMain by creating { dependsOn(commonMain) }
    val nonJvmTest by creating { dependsOn(commonTest) }
    val jvmMain by getting {
      dependencies {
        implementation(project(":packages:server"))
        api(kotlin("stdlib-jdk8"))
        api(kotlin("test-junit5"))
        api(libs.jakarta.inject)
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.jdk8)
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
      dependsOn(nonJvmMain)

      dependencies {
        api(kotlin("stdlib-js"))
        api(kotlin("test"))
        api(libs.kotlinx.coroutines.test)
        api(libs.kotlinx.coroutines.core.js)
      }
    }
    val jsTest by getting {
      dependsOn(nonJvmTest)
    }
    val nativeMain by getting {
      dependsOn(nonJvmMain)

      dependencies {
        api(kotlin("stdlib"))
      }
    }
    val nativeTest by getting {
      dependsOn(nonJvmTest)

      dependencies {
        api(kotlin("stdlib"))
        api(kotlin("test"))
      }
    }

    val nix64Main by creating { dependsOn(nativeMain) }
    val nix64Test by creating { dependsOn(nativeTest) }
    val nix32Main by creating { dependsOn(nativeMain) }
    val nix32Test by creating { dependsOn(nativeTest) }

    val wasm32Main by getting { dependsOn(nativeMain) }

    if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMac) {
      val appleMain by creating { dependsOn(nativeMain) }
      val appleTest by creating { dependsOn(nativeTest) }
      val apple64Main by creating {
        dependsOn(appleMain)
        dependsOn(nix64Main)
      }
      val apple64Test by creating {
        dependsOn(appleTest)
        dependsOn(nix64Test)
      }
      val apple32Main by creating {
        dependsOn(appleMain)
        dependsOn(nix32Main)
      }
      val apple32Test by creating {
        dependsOn(appleTest)
        dependsOn(nix32Test)
      }
      val iosX64Main by getting { dependsOn(apple64Main) }
      val iosX64Test by getting { dependsOn(apple64Test) }
      val iosArm64Main by getting { dependsOn(apple64Main) }
      val iosArm64Test by getting { dependsOn(apple64Test) }
      val macosX64Main by getting { dependsOn(apple64Main) }
      val macosX64Test by getting { dependsOn(apple64Test) }
      val macosArm64Main by getting { dependsOn(apple64Main) }
      val macosArm64Test by getting { dependsOn(apple64Test) }
      val iosArm32Main by getting { dependsOn(apple32Main) }
      val iosArm32Test by getting { dependsOn(apple32Test) }
      val iosSimulatorArm64Main by getting { dependsOn(apple64Main) }
      val iosSimulatorArm64Test by getting { dependsOn(apple64Test) }
      val watchosArm32Main by getting { dependsOn(apple32Main) }
      val watchosArm32Test by getting { dependsOn(apple32Test) }
      val watchosArm64Main by getting { dependsOn(apple64Main) }
      val watchosArm64Test by getting { dependsOn(apple64Test) }
      val watchosX64Main by getting { dependsOn(apple64Main) }
      val watchosX64Test by getting { dependsOn(apple64Test) }
      val watchosX86Main by getting { dependsOn(apple32Main) }
      val watchosX86Test by getting { dependsOn(apple32Test) }
      val watchosSimulatorArm64Main by getting { dependsOn(apple64Main) }
      val watchosSimulatorArm64Test by getting { dependsOn(apple64Test) }
      val tvosArm64Main by getting { dependsOn(apple64Main) }
      val tvosArm64Test by getting { dependsOn(apple64Test) }
      val tvosX64Main by getting { dependsOn(apple64Main) }
      val tvosX64Test by getting { dependsOn(apple64Test) }
      val tvosSimulatorArm64Main by getting { dependsOn(apple64Main) }
      val tvosSimulatorArm64Test by getting { dependsOn(apple64Test) }
    }

    if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw || org.jetbrains.kotlin.konan.target.HostManager.hostIsMac) {
      val mingwMain by creating { dependsOn(nativeMain) }
      val mingwTest by creating { dependsOn(nativeTest) }
      val mingwX64Main by getting { dependsOn(mingwMain) }
      val mingwX64Test by getting { dependsOn(mingwTest) }
    }

    if (org.jetbrains.kotlin.konan.target.HostManager.hostIsLinux || org.jetbrains.kotlin.konan.target.HostManager.hostIsMac) {
      val linuxX64Main by getting { dependsOn(nix64Main) }
      val linuxX64Test by getting { dependsOn(nix64Test) }
      val linuxArm32HfpMain by getting { dependsOn(nix32Main) }
      val linuxArm32HfpTest by getting { dependsOn(nix32Test) }
    }
  }
}
