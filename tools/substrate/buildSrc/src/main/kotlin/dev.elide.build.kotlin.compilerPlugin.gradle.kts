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

val javaToolchain: Int = (project.properties["versions.java.language"] as? String)?.toIntOrNull() ?: 17
val javaTarget: Int = (project.properties["versions.java.target"] as? String)?.toIntOrNull() ?: 17

kotlin {
  explicitApi()
  jvmToolchain(javaToolchain)
}

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
afterEvaluate {
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = ElideSubstrate.apiVersion
      languageVersion = ElideSubstrate.kotlinVerison
      jvmTarget = javaTarget.toString()
      javaParameters = true
      allWarningsAsErrors = true
      incremental = true
      freeCompilerArgs = listOf(
        "-progressive",
        "-Xcontext-receivers",
        "-Xskip-prerelease-check",
        "-Xallow-unstable-dependencies",
        "-Xemit-jvm-type-annotations",
      )
    }
  }
}

detekt {
  parallel = true
  ignoreFailures = true
}

tasks.withType<Detekt>().configureEach {
  // Target version of the generated JVM bytecode. It is used for type resolution.
  jvmTarget = javaTarget.coerceAtMost(18).toString()
}
