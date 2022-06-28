@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import java.net.URI

plugins {
  java
  jacoco
  idea
  `maven-publish`
  signing
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.micronautLibrary)
  alias(libs.plugins.testLogger)
  alias(libs.plugins.dokka)
  alias(libs.plugins.sonar)
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
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
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
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
  version.set(libs.versions.micronaut.lib.get())
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

dependencies {
  // API Deps
  api(libs.jakarta.inject)

  // Modules
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))

  // KotlinX
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Google
  implementation(libs.guava)

  // Micronaut
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)

  // GraalVM SDK
  implementation(libs.graalvm.sdk)

  // Testing
  testImplementation(project(":packages:test"))
}
