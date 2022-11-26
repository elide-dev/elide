@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

val buildDocs by properties
val enableAtomicfu = project.properties["elide.atomicFu"] == "true"

repositories {
  maven("https://maven-central.storage-download.googleapis.com/maven2/")
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.ksp)
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  if (enableAtomicfu) {
    implementation(libs.plugin.kotlinx.atomicfu)
  }
}

apply(from = "../../../gradle/loadProps.gradle")
