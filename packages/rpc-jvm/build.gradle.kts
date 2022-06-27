@file:Suppress("UNUSED_VARIABLE", "UnstableApiUsage")

import java.net.URI
import com.google.protobuf.gradle.*

val protobufVersion = project.properties["versions.protobuf"] as String
val grpcVersion = project.properties["versions.grpc"] as String
val grpcKotlinVersion = project.properties["versions.grpcKotlin"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String
val junitJupiterVersion =  project.properties["versions.junit.jupiter"] as String
val micronautTestVersion = project.properties["versions.micronaut.test"] as String
val logbackVersion = project.properties["versions.logback"] as String

plugins {
  java
  idea
  jacoco
  signing
  `jvm-test-suite`
  `maven-publish`
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
  id("com.adarshr.test-logger")
  id("com.google.protobuf")
  id("io.micronaut.library")
  id("org.jetbrains.dokka")
  id("org.sonarqube")
}

group = "dev.elide"
version = rootProject.version as String

micronaut {
  version.set(Versions.micronaut)
  processing {
    incremental.set(true)
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${Versions.protobuf}"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}"
    }
    id("grpckt") {
      artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.grpcKotlin}:jdk8@jar"
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
    languageVersion.set(JavaLanguageVersion.of(Versions.javaLanguage))
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

  implementation("io.grpc:grpc-core:$grpcVersion")
  implementation("io.grpc:grpc-api:$grpcVersion")
  implementation("io.grpc:grpc-auth:$grpcVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
  implementation("io.grpc:grpc-services:$grpcVersion")
  implementation("io.grpc:grpc-netty:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")

  implementation("com.google.protobuf:protobuf-java:${Versions.protobuf}")
  implementation("com.google.protobuf:protobuf-java-util:${Versions.protobuf}")
  implementation("com.google.protobuf:protobuf-kotlin:${Versions.protobuf}")

  implementation("io.micronaut:micronaut-http:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-context:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-inject-java:${Versions.micronaut}")
  implementation("io.micronaut:micronaut-management:${Versions.micronaut}")
  implementation("io.micronaut.grpc:micronaut-grpc-runtime:${Versions.micronautGrpc}")
  implementation("io.micronaut.grpc:micronaut-grpc-client-runtime:${Versions.micronautGrpc}")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:$kotlinxSerializationVersion")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:${Versions.coroutinesVersion}")

  // Testing
  testImplementation(project(":packages:test"))
  testImplementation(kotlin("test-junit5"))
  testImplementation("io.micronaut.test:micronaut-test-junit5:$micronautTestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
  testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
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
