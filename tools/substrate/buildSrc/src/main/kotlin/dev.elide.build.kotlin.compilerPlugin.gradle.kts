import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`

  kotlin("jvm")
  kotlin("kapt")
  id("com.github.gmazzo.buildconfig")
  id("io.gitlab.arturbosch.detekt")
  id("dev.elide.build.substrate")
}

group = "dev.tools.compiler.plugin"
version = rootProject.version as String

java {
  sourceCompatibility = JavaVersion.VERSION_20
  targetCompatibility = JavaVersion.VERSION_20
}

kotlin {
  explicitApi()

  sourceSets.all {
    languageSettings.apiVersion = ElideSubstrate.KOTLIN_VERSION
    languageSettings.languageVersion = ElideSubstrate.KOTLIN_VERSION
  }
}

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = ElideSubstrate.API_VERSION
    languageVersion = ElideSubstrate.KOTLIN_VERSION
    jvmTarget = "20"
    javaParameters = true
    allWarningsAsErrors = true
    incremental = true
    freeCompilerArgs = freeCompilerArgs.plus(listOf(
      "-Xallow-unstable-dependencies",
    ))
  }
}

detekt {
  parallel = true
  ignoreFailures = true
  config.from(rootProject.files("../../config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
  // Target version of the generated JVM bytecode. It is used for type resolution.
  jvmTarget = "18"
}
