plugins {
  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
}

group = "dev.tools.compiler.plugin"
version = rootProject.version as String

kotlin {
  explicitApi()
}

