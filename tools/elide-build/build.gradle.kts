@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  `kotlin-dsl`
}

group = "dev.elide.tools"
version = rootProject.version as String

gradlePlugin {
  plugins {
    create("elideInternalBuild") {
      id = "elide.internal.conventions"
      implementationClass = "elide.internal.conventions.ElideConventionPlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  explicitApi()
  
  compilerOptions {
    jvmTarget = JVM_17
    javaParameters = true
    allWarningsAsErrors = true
    
    apiVersion = KOTLIN_1_9
    languageVersion = KOTLIN_1_9
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    languageVersion = "1.9"
  }
}

dependencies {
  implementation(gradleApi())

  // included plugins
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.kotlin)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.sigstore)
  implementation(libs.plugin.redacted)
  implementation(libs.plugin.ksp)

  // embedded Kotlin plugins
  implementation(embeddedKotlin("allopen"))
  implementation(embeddedKotlin("noarg"))
  implementation(embeddedKotlin("serialization"))
}

configurations.all {
  resolutionStrategy {
    // fail eagerly on version conflict (includes transitive dependencies)
    failOnVersionConflict()

    // prefer modules that are part of this build
    preferProjectModules()
  }
}
