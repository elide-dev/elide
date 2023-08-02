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
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  explicitApi()
}

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = ElideSubstrate.apiVersion
    languageVersion = ElideSubstrate.kotlinVerison
    jvmTarget = "11"
    javaParameters = true
    allWarningsAsErrors = false
    incremental = true
  }
}

detekt {
  parallel = true
  ignoreFailures = true
}

tasks.withType<Detekt>().configureEach {
  // Target version of the generated JVM bytecode. It is used for type resolution.
  jvmTarget = "11"
}
