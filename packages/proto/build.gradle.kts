@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.*

plugins {
  id("dev.elide.build.jvm")
  id("dev.elide.build.kotlin")
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String

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
  compileOnly(libs.google.cloud.nativeImageSupport)
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageVersion
  targetCompatibility = javaLanguageVersion
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
