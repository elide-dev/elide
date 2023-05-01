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

kotlin {
  explicitApi()
  jvmToolchain(17)
}

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = ElideSubstrate.apiVersion
    languageVersion = ElideSubstrate.kotlinVerison
    jvmTarget = "17"
    javaParameters = true
    allWarningsAsErrors = true
    incremental = true
  }
}

detekt {
  parallel = true
  ignoreFailures = true
}

tasks.withType<Detekt>().configureEach {
  // Target version of the generated JVM bytecode. It is used for type resolution.
  jvmTarget = "17"
}
