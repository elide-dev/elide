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
  api(npm("@emotion/css", libs.versions.npm.emotion.core.get()))
  api(npm("@emotion/server", libs.versions.npm.emotion.server.get()))

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.wrappers.node)
  implementation(libs.kotlinx.wrappers.emotion)
  implementation(libs.kotlinx.wrappers.history)
  implementation(libs.kotlinx.wrappers.typescript)
  implementation(libs.kotlinx.wrappers.react.router.dom)
  implementation(libs.kotlinx.wrappers.remix.run.router)

  // Testing
  testImplementation(project(":packages:test"))
}
