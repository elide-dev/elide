@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
  "OPT_IN_USAGE",
)

import Java9Modularity.configureJava9ModuleInfo

plugins {
  kotlin("plugin.noarg")
  kotlin("plugin.allopen")
  kotlin("plugin.serialization")
  id("dev.elide.internal.kotlin.redakt")
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"

kotlin {
  explicitApi()

  jvm {
    compilations.all {
      kotlinOptions {
        apiVersion = libs.versions.kotlin.language.get()
        languageVersion = libs.versions.kotlin.language.get()
      }
    }
    withJava()
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }
  js(IR) {
    browser {}
    nodejs {}
    binaries.executable()
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

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(project(":packages:base"))
        api(project(":packages:core"))
        api(libs.kotlinx.collections.immutable)
        api(libs.kotlinx.datetime)
        api(libs.kotlinx.serialization.core)
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.serialization.protobuf)
        api(libs.kotlinx.coroutines.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(project(":packages:base"))
        implementation(project(":packages:proto:proto-core"))
        implementation(project(":packages:proto:proto-protobuf"))
        implementation(project(":packages:proto:proto-kotlinx"))
        implementation(libs.jakarta.inject)
        api(libs.protobuf.java)
        api(libs.protobuf.kotlin)
        api(libs.flatbuffers.java.core)
        implementation(libs.kotlinx.serialization.json.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)
        implementation(libs.kotlinx.coroutines.core.jvm)
        implementation(libs.kotlinx.coroutines.jdk9)
        implementation(libs.kotlinx.coroutines.guava)
        implementation(libs.google.api.common)
        implementation(libs.reactivestreams)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit5"))
        implementation(project(":packages:base"))
        implementation(libs.truth)
        implementation(libs.truth.proto)
        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(project(":packages:base"))
        implementation(project(":packages:frontend"))
        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }
    val jsTest by getting
    val nativeMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(project(":packages:base"))
      }
    }
    val nativeTest by getting

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

configureJava9ModuleInfo(
  multiRelease = true,
)

if (project.properties["buildDocs"] == "true") {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
  }
}

publishing {
  publications.withType<MavenPublication> {
    artifact("javadocJar")
    artifactId = artifactId.replace("model", "elide-model")

    pom {
      name.set("Elide Model")
      url.set("https://elide.dev")
      description.set(
        "Data and structure modeling runtime package for use with the Elide Framework."
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
