@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js")
}

group = "dev.elide"
version = rootProject.version as String

dependencies {
  implementation(project(":packages:base"))
  implementation(project(":packages:frontend"))
  implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
  implementation(npm("google-protobuf", libs.versions.protobuf.get()))
  implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.serialization.protobuf.js)

  // Testing
  testImplementation(project(":packages:test"))
}
