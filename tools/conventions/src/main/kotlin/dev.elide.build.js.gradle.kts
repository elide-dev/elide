import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
  kotlin("kapt")
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("org.jetbrains.kotlinx.kover")
  id("dev.elide.build.core")
}

val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val strictMode = project.properties["strictMode"] as? String == "true"

// Compiler: Kotlin
// ----------------
// Settings for compiling Kotlin to JavaScript.
kotlin {
  org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories.getProvider(project)

  js(IR) {
    browser()
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = project.name
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
    onCheck = true
  }
}

// Sources: Kotlin
// ---------------
// Shared configuration for Kotlin language and compiler.
kotlin {
  sourceSets.all {
    languageSettings.apply {
      apiVersion = kotlinLanguageVersion
      languageVersion = kotlinLanguageVersion
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }
}

tasks.withType<KotlinCompileCommon>().configureEach {
  kotlinOptions.apply {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    freeCompilerArgs = Elide.jsCompilerArgs
    allWarningsAsErrors = strictMode
  }
}
