@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.*

plugins {
  id("dev.elide.build.jvm")
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

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
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)
  implementation(libs.google.common.html.types.proto)
  compileOnly(libs.google.cloud.nativeImageSupport)
}
