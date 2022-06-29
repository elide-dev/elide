@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import java.net.URI
import com.google.protobuf.gradle.*

plugins {
  java
  jacoco
  idea
  `maven-publish`
  signing
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  alias(libs.plugins.testLogger)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.micronautLibrary)
  alias(libs.plugins.dokka)
  alias(libs.plugins.sonar)
}

group = "dev.elide"
version = rootProject.version as String

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${Versions.protobuf}"
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.builtins {
        id("kotlin")
      }
    }
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
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
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
}

signing {
  if (project.hasProperty("enableSigning") && project.properties["enableSigning"] == "true") {
    sign(configurations.archives.get())
  }
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
  version.set(libs.versions.micronaut.lib.get())
}

sourceSets {
  named("main") {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
}

dependencies {
  // API Deps
  api(libs.jakarta.inject)
  api(libs.slf4j)
  api(platform(libs.grpc.bom))
  api(platform(libs.netty.bom))

  // Protocol Buffers
  protobuf(files("${rootProject.projectDir}/proto/deps/webutil.tar.gz"))

  // Modules
  implementation(project(":packages:base"))

  // Kotlin
  implementation(libs.kotlinx.html.jvm)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Kotlin Wrappers
  implementation(libs.kotlinx.wrappers.css)

  // Protocol Buffers
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // Google
  implementation(libs.grpc.core)
  implementation(libs.grpc.api)
  implementation(libs.grpc.auth)
  implementation(libs.grpc.stub)
  implementation(libs.grpc.services)
  implementation(libs.grpc.netty)
  implementation(libs.grpc.protobuf)
  implementation(libs.grpc.kotlin.stub)
  implementation(libs.guava)

  // Micronaut
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.grpc.runtime)
  implementation(libs.micronaut.grpc.client.runtime)
  implementation(libs.micronaut.grpc.server.runtime)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.kotlinx.coroutines.slf4j)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.kotlinx.coroutines.reactive)

  // Testing
  testImplementation(project(":packages:test"))
}
