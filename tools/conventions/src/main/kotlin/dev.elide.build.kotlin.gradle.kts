import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  publishing

  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

val defaultJavaVersion = "11"
val defaultKotlinVersion = "1.7"

val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"
val javaLanguageVersion = project.properties["versions.java.language"] as? String ?: defaultJavaVersion
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageTarget
    javaParameters = true
    freeCompilerArgs = Elide.kaptCompilerArgs
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageTarget)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
  }

  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }
}

// Tool: Kover
// -----------
// Settings for Kotlin coverage.
kover {
  xmlReport {
    onCheck.set(true)
  }
}
