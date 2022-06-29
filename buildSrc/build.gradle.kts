val kotlinVersion = "1.7.0"

plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  api(kotlin("gradle-plugin"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
