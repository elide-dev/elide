@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.jvm")
  id("com.google.devtools.ksp")
}

group = "dev.elide"
version = rootProject.version as String


kotlin {
  explicitApi()
  jvmToolchain(11)  // force because of gradle min
}

dependencies {
  // Core platform versions.
  ksp(libs.autoService.ksp)

  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)
  api(libs.graalvm.sdk)

  // Protocol dependencies.
  implementation(project(":packages:proto:proto-core"))
  implementation(project(":packages:proto:proto-protobuf"))
  implementation(project(":packages:proto:proto-kotlinx"))

  // Modules
  implementation(project(":packages:base"))

  // KSP
  implementation(libs.ksp)
  implementation(libs.ksp.api)
  implementation(libs.google.auto.service)
  implementation(libs.kotlinx.atomicfu)

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)
  implementation(libs.errorprone.annotations)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactive)

  // Testing
  testImplementation(kotlin("test"))
  testImplementation(project(":packages:test"))
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      group = "dev.elide"
      artifactId = "elide-ksp-processor"
      version = rootProject.version as String
      from(components["kotlin"])

      pom {
        name.set("Elide Annotation Processor for KSP")
        url.set("https://github.com/elide-dev/elide")
        description.set(
          "Generates code from annotations using Kotlin Symbol Processing (KSP)."
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
}
