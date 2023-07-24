@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.9.20-station-823"

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  `embedded-kotlin`
}

val buildDocs by properties
val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

dependencies {
  implementation(gradleApi())
  api(libs.elide.tools.conventions)
  implementation(libs.elide.kotlin.plugin.redakt)
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.micronaut)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.shadow)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.plugin.ksp)
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

afterEvaluate {
  tasks {
    compileKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }

    compileTestKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }
  }
}

apply(from = "../gradle/loadProps.gradle.kts")
