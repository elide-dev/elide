import java.net.URI

val protobufVersion = project.properties["versions.protobuf"] as String
val grpcVersion = project.properties["versions.grpc"] as String
val grpcKotlinVersion = project.properties["versions.grpcKotlin"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String

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

micronaut {
  version.set(Versions.micronaut)
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
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxSerializationVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm:$kotlinxSerializationVersion")
}
