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

kotlin {
  explicitApi()
}

dependencies {
  api(npm("esbuild", libs.versions.npm.esbuild.get()))
  api(npm("prepack", libs.versions.npm.prepack.get()))
  api(npm("buffer", libs.versions.npm.buffer.get()))
  api(npm("readable-stream", libs.versions.npm.stream.get()))

  implementation(libs.kotlinx.wrappers.node)

  // Testing
  testImplementation(project(":packages:test"))
}
