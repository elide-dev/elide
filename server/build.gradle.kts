@file:Suppress("UnstableApiUsage", "unused", "UNUSED_VARIABLE")

import java.net.URI

plugins {
  java
  jacoco
  idea
  `maven-publish`
  signing
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  id("io.micronaut.library")
  id("org.jetbrains.dokka")
  id("org.sonarqube")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.javaLanguage))
  }
  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = "server"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
}

publishing {
  repositories {
    maven {
      name = "elide"
      url = URI.create(project.properties["elide.publish.repo.maven"] as String)

      if (project.hasProperty("elide.publish.repo.maven.auth")) {
          credentials {
              username = (project.properties["elide.publish.repo.maven.username"] as? String
                  ?: System.getenv("PUBLISH_USER"))?.ifBlank { null }
              password = (project.properties["elide.publish.repo.maven.password"] as? String
                  ?: System.getenv("PUBLISH_TOKEN"))?.ifBlank { null }
          }
      }
    }
  }

  publications.withType<MavenPublication> {
    artifact(javadocJar.get())
    pom {
      name.set("Elide")
      description.set("Polyglot application framework")
      url.set("https://github.com/elide-dev/v3")

      licenses {
        license {
          name.set("Properity License")
          url.set("https://github.com/elide-dev/v3/blob/v3/LICENSE")
        }
      }
      developers {
        developer {
          id.set("sgammon")
          name.set("Sam Gammon")
          email.set("samuel.gammon@gmail.com")
        }
      }
      scm {
        url.set("https://github.com/elide-dev/v3")
      }
    }
  }
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
  }
  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "**/generated/**",
          "**/com/**",
          "**/grpc/gateway/**",
          "**/tools/elide/**",
        )
      }
    })
  )
}

micronaut {
  version.set(Versions.micronaut)
}

dependencies {
  // API Deps
  api("jakarta.inject:jakarta.inject-api:2.0.1")
  api("org.slf4j:slf4j-api:${Versions.slf4j}")
  api(platform("io.netty:netty-bom:${Versions.netty}"))
  api(platform("io.grpc:grpc-bom:${Versions.grpc}"))
  api(platform("io.netty:netty-bom:${Versions.netty}"))

  // Modules
  implementation(project(":base"))

  // Kotlin
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:${Versions.kotlinSerialization}")

  // Kotlin Wrappers
  implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-${Versions.kotlinWrappers}")

  // Google
  implementation("io.grpc:grpc-core")
  implementation("io.grpc:grpc-api")
  implementation("io.grpc:grpc-auth")
  implementation("io.grpc:grpc-stub")
  implementation("io.grpc:grpc-services")
  implementation("io.grpc:grpc-netty")
  implementation("io.grpc:grpc-protobuf")
  implementation("io.grpc:grpc-kotlin-stub:${Versions.grpcKotlin}")
  implementation("com.google.guava:guava:${Versions.guava}")

  // Protocol Buffers
  implementation("com.google.protobuf:protobuf-java:${Versions.protobuf}")
  implementation("com.google.protobuf:protobuf-kotlin:${Versions.protobuf}")

  // Micronaut
  implementation("io.micronaut:micronaut-http:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-context:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject-java:${Versions.micronaut}")
  implementation("io.micronaut.grpc:micronaut-grpc-runtime:${Versions.micronautGrpc}")
  implementation("io.micronaut.grpc:micronaut-grpc-client-runtime:${Versions.micronautGrpc}")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.coroutinesVersion}")
}
