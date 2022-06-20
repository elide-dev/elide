@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

plugins {
  java
  jacoco
  idea
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("io.micronaut.application")
  id("io.micronaut.aot")
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.javaLanguage))
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

application {
  mainClass.set("helloworld.App")
}

micronaut {
  version.set(Versions.micronaut)
  runtime.set(io.micronaut.gradle.MicronautRuntime.NETTY)
  processing {
    incremental.set(true)
    annotations.add("helloworld.*")
  }
  aot {
    optimizeServiceLoading.set(true)
    convertYamlToJava.set(true)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    netty {
      enabled.set(true)
    }
  }
}

dependencies {
  implementation(project(":server"))
  implementation("io.micronaut:micronaut-context")
  implementation("io.micronaut:micronaut-runtime")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  runtimeOnly("ch.qos.logback:logback-classic")
}

tasks.withType<Copy>().named("processResources") {
  dependsOn("copyStatic")
}

tasks.register<Copy>("copyStatic") {
  from("src/main/resources/static/**/*.*")
  into("$buildDir/resources/main/static")
}
