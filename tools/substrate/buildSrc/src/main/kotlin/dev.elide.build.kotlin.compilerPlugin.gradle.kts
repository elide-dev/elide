plugins {
  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
  id("io.gitlab.arturbosch.detekt")
  id("dev.elide.build.substrate")
}

group = "dev.tools.compiler.plugin"
version = rootProject.version as String

kotlin {
  explicitApi()
}

detekt {
  parallel = true
  ignoreFailures = true
}
