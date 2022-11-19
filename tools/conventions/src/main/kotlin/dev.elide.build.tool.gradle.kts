
plugins {
  java
  jacoco
  application
  `jvm-test-suite`

  kotlin("jvm")
  kotlin("plugin.serialization")
  id("dev.elide.build.core")
  id("dev.elide.build.kotlin")
  id("dev.elide.build.jvm.kapt")
}
