import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  publishing

  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

val strictMode = project.properties["versions.java.language"] as String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as String == "true"
val javaLanguageVersion = project.properties["versions.java.language"] as String
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String

// Compiler: Kotlin
// ----------------
// Configure Kotlin compile runs for MPP, JS, and JVM.
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageVersion
    javaParameters = true
    freeCompilerArgs = Elide.kaptCompilerArgs
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
  }

  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        version = rootProject.version as String
        from(components["kotlin"])
      }
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
