@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "unused_variable",
  "DSL_SCOPE_VIOLATION",
)
@file:OptIn(
  org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class
)

import Java9Modularity.configure as configureJava9ModuleInfo

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()

  js(IR) {
    browser()
    nodejs()
  }
  wasm {
    browser()
    nodejs()
    d8()
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
    val jsMain by getting {
      dependencies {
        // KT-57235: fix for atomicfu-runtime error
        api("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:1.8.20-RC")
      }
    }
    val jsTest by getting
  }
}

configureJava9ModuleInfo(project)

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
  }
  javadocJar
} else null

publishing {
  publications.withType<MavenPublication> {
    if (buildDocs) {
      artifact(javadocJar)
    }
    artifactId = artifactId.replace("ssr", "elide-ssr")

    pom {
      name.set("Elide SSR")
      url.set("https://elide.dev")
      description.set(
        "Package for server-side rendering (SSR) capabilities with the Elide Framework."
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
