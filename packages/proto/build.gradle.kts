@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.*

plugins {
  `maven-publish`
  distribution
  signing
  id("dev.elide.build.kotlin")
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.builtins {
        id("kotlin")
      }
    }
  }
}

afterEvaluate {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = false
  }
}

sourceSets {
  named("main") {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
}

dependencies {
  api(libs.protobuf.java)
  api(libs.protobuf.util)
  api(libs.protobuf.kotlin)
  api(libs.google.common.html.types.proto)
  api(libs.google.common.html.types.types)
  api(libs.kotlinx.datetime)
  implementation(kotlin("reflect"))
  compileOnly(libs.google.cloud.nativeImageSupport)
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  compileKotlin {
    kotlinOptions.freeCompilerArgs = Elide.jvmCompilerArgs.plus(listOf(
      // do not warn for generated code
      "-nowarn"
    ))
  }
}
