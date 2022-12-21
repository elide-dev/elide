@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import io.netifi.flatbuffers.plugin.tasks.FlatBuffers
import com.google.protobuf.gradle.*

plugins {
  `maven-publish`
  distribution
  signing
  id("dev.elide.build.kotlin")
  alias(libs.plugins.protobuf)
  alias(libs.plugins.flatbuffers)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  generateProtoTasks {
    ofSourceSet("main").forEach {
      it.builtins {
        id("kotlin")
      }
    }
  }
}

flatbuffers {
  language = "kotlin"
}

afterEvaluate {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = false
  }
}

sourceSets {
  named("main") {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
}

kotlin {
  sourceSets {
    val main by getting {
      kotlin.srcDir("$buildDir/generated/source/flatbuffers")
    }
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}

dependencies {
  api(libs.protobuf.java)
  api(libs.protobuf.kotlin)
  api(libs.flatbuffers.java.core)
  api(libs.kotlinx.datetime)
  implementation(kotlin("reflect"))
  compileOnly(libs.google.cloud.nativeImageSupport)

  // Safe HTML Types
  implementation(libs.google.common.html.types.proto)
  implementation(libs.google.common.html.types.types)
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  compileKotlin {
    dependsOn(
      "compileFlatbuffers"
    )

    kotlinOptions.freeCompilerArgs = Elide.jvmCompilerArgs.plus(listOf(
      // do not warn for generated code
      "-nowarn"
    ))
  }

  create("compileFlatbuffers", FlatBuffers::class) {
    description = "Generate Flatbuffers code for Kotlin/JVM"
    inputDir = file("${rootProject.projectDir}/proto")
    outputDir = file("$buildDir/generated/source/flatbuffers")
  }
}
