@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

import java.util.Properties

plugins {
  kotlin("plugin.serialization") version "1.7.0" apply false
  id("org.jetbrains.kotlinx.kover") version "0.5.0"
}

val props = java.util.Properties()
props.load(file(if (project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true") {
  "gradle-ci.properties"
} else {
  "local.properties"
}).inputStream())

buildscript {
  repositories {
    google()
    mavenCentral()
    maven("https://maven-central.storage-download.googleapis.com/maven2/")
    maven("https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath("com.bmuschko:gradle-docker-plugin:${Versions.dockerPlugin}")
    classpath("com.github.node-gradle:gradle-node-plugin:${Versions.nodePlugin}")
    classpath("io.micronaut.gradle:micronaut-gradle-plugin:${Versions.micronautPlugin}")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    classpath("org.jetbrains.kotlinx:kover:${Versions.koverPlugin}")
    classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Versions.atomicfuPlugin}")
    classpath("com.adarshr:gradle-test-logger-plugin:${Versions.testLoggerPlugin}")
  }
  if (project.property("elide.lockDeps") == "true") {
    configurations.classpath {
      resolutionStrategy.activateDependencyLocking()
    }
  }
}

tasks.register("relock") {
  dependsOn(
    *(subprojects.map {
      it.tasks.named("dependencies")
    }.toTypedArray())
  )
}

if (project.property("elide.lockDeps") == "true") {
  subprojects {
    dependencyLocking {
      lockAllConfigurations()
    }
  }
}

allprojects {
  repositories {
    google()
    mavenCentral()
    jcenter()
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
      jvmTarget = Versions.javaLanguage
      javaParameters = true
    }
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions {
      apiVersion = Versions.kotlinLanguage
      languageVersion = Versions.kotlinLanguage
    }
  }
}
