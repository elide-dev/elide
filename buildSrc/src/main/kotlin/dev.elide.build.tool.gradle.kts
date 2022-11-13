
plugins {
  java
  jacoco
  application
  `jvm-test-suite`

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("dev.elide.build.core")
  id("dev.elide.build.jvm")
}

// Compiler: `kapt`
// ----------------
// Configure Kotlin annotation processing.
kapt {
  useBuildCache = true
}
