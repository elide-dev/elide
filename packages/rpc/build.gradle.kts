@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

import com.google.protobuf.gradle.id

plugins {
  id("dev.elide.build")
  id("dev.elide.build.multiplatform")
  `java`
  kotlin("kapt")
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

val ideaActive = properties["idea.active"] == "true"

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
    all().forEach {
      it.addIncludeDir(files("$projectDir/src/jvmTest/proto"))
      it.addSourceDirs(files("$projectDir/src/jvmTest/proto"))

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
  explicitApi()

  jvm {
    withJava()
  }
  js(IR) {
    nodejs {}
    browser {}
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":packages:base"))
        api(project(":packages:core"))
        api(project(":packages:model"))
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("test"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(project(":packages:base"))
        implementation(project(":packages:server"))
        configurations["kapt"].dependencies.add(libs.micronaut.inject.java.asProvider().get())

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
        implementation(libs.kotlinx.coroutines.guava)
      }
    }
    val jvmTest by getting {
      kotlin.srcDirs(
        layout.projectDirectory.dir("src/jvmTest/kotlin"),
        layout.buildDirectory.dir("generated/source/proto/test/grpckt"),
        layout.buildDirectory.dir("generated/source/proto/test/kotlin"),
      )

      dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("test-junit5"))
        configurations["kaptTest"].dependencies.add(libs.micronaut.inject.java.asProvider().get())

        // Testing
        implementation(project(":packages:test"))
        implementation(kotlin("test-junit5"))
        implementation(libs.micronaut.test.junit5)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        runtimeOnly(libs.junit.jupiter.engine)
        runtimeOnly(libs.logback)
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(project(":packages:base"))
        implementation(project(":packages:frontend"))
        implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
        implementation(npm("google-protobuf", libs.versions.npm.google.protobuf.get()))
        implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(kotlin("test"))
        implementation(project(":packages:test"))
      }
    }
  }
}

tasks {
  withType(Copy::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
  withType(Jar::class).configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  named("compileTestKotlinJvm").configure {
    dependsOn("generateProto", "generateTestProto")
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier = "javadoc"
    from(dokkaHtml.outputDirectory)
  }
  javadocJar
} else null

afterEvaluate {
  listOf(
    "kaptGenerateStubsKotlinJvm",
    "kaptGenerateStubsTestKotlinJvm",
    "runKtlintCheckOverJvmTestSourceSet",
  ).forEach {
    tasks.named(it) {
      dependsOn(tasks.generateProto, tasks.generateTestProto)
    }
  }
}

publishing {
  publications.withType<MavenPublication> {
    if (buildDocs) {
      artifact(javadocJar)
    }
    artifactId = artifactId.replace("rpc", "elide-rpc")

    pom {
      name = "Elide RPC"
      url = "https://elide.dev"
      description = "Cross-platform RPC dispatch and definition tools and runtime utilities"

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}
