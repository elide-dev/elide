plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

val kotlinVersion = "1.7.21"

repositories {
  maven("https://maven-central.storage-download.googleapis.com/maven2/")
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  api(kotlin("gradle-plugin"))
  implementation(libs.plugin.buildConfig)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

afterEvaluate {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = "1.6"
      languageVersion = "1.6"
      jvmTarget = "11"
      javaParameters = true
      allWarningsAsErrors = true
      incremental = true
    }
  }
}
