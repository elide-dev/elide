val kotlinVersion = "1.7.21"

plugins {
  `kotlin-dsl`
}

repositories {
  maven("https://maven-central.storage-download.googleapis.com/maven2/")
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  api(kotlin("gradle-plugin"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
