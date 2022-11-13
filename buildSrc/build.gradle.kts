@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.7.21"

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  maven("https://maven-central.storage-download.googleapis.com/maven2/")
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.micronaut)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.atomicfu)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
