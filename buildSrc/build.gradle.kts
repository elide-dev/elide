val kotlinVersion = "1.7.0"
val micronautPluginVersion = "3.4.1"

plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation("io.micronaut.gradle:micronaut-gradle-plugin:$micronautPluginVersion")
}
