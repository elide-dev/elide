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
  id("com.adarshr.test-logger")
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
        artifactId = "graalvm"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.GRAAL_VM)
    if (project.hasProperty("elide.graalvm.variant")) {
      val variant = project.property("elide.graalvm.variant") as String
      if (variant != "COMMUNITY") {
        vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
          "ENTERPRISE" -> "GraalVM Enterprise"
          else -> "GraalVM Community"
        }))
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

micronaut {
  version.set(Versions.micronaut)
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
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

// add to graalvm flags:
// -Dpolyglot.image-build-time.PreinitializeContexts=js

dependencies {
  // API Deps
  api("jakarta.inject:jakarta.inject-api:2.0.1")

  // Modules
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))

  // KotlinX
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:${Versions.kotlinxHtml}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:${Versions.kotlinSerialization}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:${Versions.coroutinesVersion}")

  // Google
  implementation("com.google.guava:guava:${Versions.guava}")

  // Micronaut
  implementation("io.micronaut:micronaut-http:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-context:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject-java:${Versions.micronaut}")

  // GraalVM SDK
  implementation("org.graalvm.sdk:graal-sdk:${Versions.graalvm}")

  // Testing
  testImplementation(project(":packages:test"))
}
