
plugins {
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("dev.elide.build.core")
}

val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String


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
