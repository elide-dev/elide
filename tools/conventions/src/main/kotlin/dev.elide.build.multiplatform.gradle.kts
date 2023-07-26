@file:Suppress(
  "UNUSED_VARIABLE",
)

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("dev.elide.build.core")
  id("org.jetbrains.kotlinx.kover")
}

val defaultJavaVersion = "17"
val defaultKotlinVersion = "1.9"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"

kover {
  xmlReport {
    onCheck.set(
      project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"
    )
  }
}

kotlin {
  jvm {
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }

  js(IR) {
    browser {
      commonWebpackConfig {
        cssSupport {
          enabled.set(true)
        }
      }
    }
  }

  if (project.hasProperty("publishMainHostLock") && project.properties["publishMainHostLock"] == "true") {
    val publicationsFromMainHost =
      listOf(jvm(), js()).map { it.name } + "kotlinMultiplatform"

    publishing {
      publications {
        matching { it.name in publicationsFromMainHost }.all {
          val targetPublication = this@all
          tasks.withType<AbstractPublishToMaven>()
            .matching { it.publication == targetPublication }
            .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
        }
      }
    }
  }

  val hostOs = System.getProperty("os.name")
  val isMingwX64 = hostOs.startsWith("Windows")
  val nativeTarget = when {
    hostOs == "Mac OS X" -> macosX64("native")
    hostOs == "Linux" -> linuxX64("native")
    isMingwX64 -> mingwX64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
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

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
}

tasks.withType<KotlinCompileCommon>().configureEach {
  kotlinOptions {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    freeCompilerArgs = Elide.mppCompilerArgs
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = kotlinLanguageVersion
    languageVersion = kotlinLanguageVersion
    jvmTarget = javaLanguageTarget
    freeCompilerArgs = Elide.mppCompilerArgs
    javaParameters = true
    allWarningsAsErrors = strictMode
    incremental = true
  }
}
