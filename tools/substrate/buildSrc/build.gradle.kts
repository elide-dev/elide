@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

val buildDocs by properties
val enableAtomicfu = project.properties["elide.atomicFu"] == "true"
val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

repositories {
  maven("https://maven.pkg.st/")
  maven("https://gradle.pkg.st/")
}

dependencies {
  implementation(gradleApi())
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.ksp)
  implementation(embeddedKotlin("allopen"))
  implementation(embeddedKotlin("noarg"))
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.kotlinx.abiValidator)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  if (enableAtomicfu) {
    implementation(libs.plugin.kotlinx.atomicfu)
  }
}

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

afterEvaluate {
  tasks {
    compileKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }

    compileTestKotlin.configure {
      kotlinOptions {
        jvmTarget = javaLanguageTarget
        javaParameters = true
      }
    }
  }
}

apply(from = "../../../gradle/loadProps.gradle.kts")
