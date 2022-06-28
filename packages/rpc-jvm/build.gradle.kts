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
  idea
  jacoco
  signing
  `jvm-test-suite`
  `maven-publish`
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

micronaut {
  version.set(libs.versions.micronaut.lib.get())
  processing {
    incremental.set(true)
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.java.get()}"
    }
    id("grpckt") {
      artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpc.kotlin.get()}:jdk8@jar"
    }
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.plugins {
        id("grpc")
        id("grpckt")
      }
      it.builtins {
        id("kotlin")
      }
    }
    ofSourceSet("test").forEach {
      it.plugins {
        id("grpc")
        id("grpckt")
      }
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
        artifactId = "rpc-jvm"
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

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
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

dependencies {
  implementation(project(":packages:base"))
  implementation(project(":packages:server"))

  // Protobuf
  implementation(libs.protobuf.java)
  implementation(libs.protobuf.util)
  implementation(libs.protobuf.kotlin)

  // gRPC
  implementation(libs.grpc.api)
  implementation(libs.grpc.auth)
  implementation(libs.grpc.core)
  implementation(libs.grpc.stub)
  implementation(libs.grpc.services)
  implementation(libs.grpc.netty)
  implementation(libs.grpc.protobuf)
  implementation(libs.grpc.kotlin.stub)

  // Micronaut
  implementation(libs.micronaut.http)
  implementation(libs.micronaut.context)
  implementation(libs.micronaut.inject)
  implementation(libs.micronaut.inject.java)
  implementation(libs.micronaut.management)
  implementation(libs.micronaut.grpc.runtime)
  implementation(libs.micronaut.grpc.client.runtime)
  implementation(libs.micronaut.grpc.server.runtime)

  // Serialization
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)
  implementation(libs.kotlinx.serialization.protobuf.jvm)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.core.jvm)
  implementation(libs.kotlinx.coroutines.jdk8)
  implementation(libs.kotlinx.coroutines.guava)

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.micronaut.test.junit5)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.logback)
}

tasks.jacocoTestReport {
  reports {
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.dokkaHtml.configure {
  moduleName.set("rpc-jvm")
}
