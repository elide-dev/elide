@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)


plugins {
  java
  jacoco
  `jvm-test-suite`
  `maven-publish`

  id("com.github.gmazzo.buildconfig")
  kotlin("kapt")
  id("dev.elide.build.core")
  id("dev.elide.build.kotlin")
}

group = "dev.tools.compiler"
version = rootProject.version as String


val javaLanguageVersion = project.properties["versions.java.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["versions.java.language"] as String == "true"

// Compiler: Kotlin
// ----------------
// Override with JVM-specific (non-kapt) arguments.
kotlin {
  explicitApi()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageVersion
    javaParameters = true
    freeCompilerArgs = Elide.kaptCompilerArgs
    allWarningsAsErrors = true
    incremental = true
  }
}

// Compiler: `kapt`
// ----------------
// Configure Kotlin annotation processing.
kapt {
  useBuildCache = true
  includeCompileClasspath = false
  strictMode = true
}

dependencies {
  api(platform(project(":packages:platform")))
  implementation(project(":tools:compiler:compiler-util"))
  testImplementation(kotlin("test"))
  testImplementation(project(":tools:compiler:compiler-util", "test"))
}
