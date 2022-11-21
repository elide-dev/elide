@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.7.21"

plugins {
  id("dev.elide.build")

  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

val buildDocs by properties

dependencies {
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.detekt)
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
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

apply(from = "../gradle/loadProps.gradle")
