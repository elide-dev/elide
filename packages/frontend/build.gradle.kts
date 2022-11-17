@file:Suppress(
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
  implementation(kotlin("stdlib-js"))
  implementation(project(":packages:base"))

  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.serialization.protobuf.js)

  // Testing
  testImplementation(project(":packages:test"))
}
