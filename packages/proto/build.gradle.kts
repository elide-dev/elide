@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION", "UNUSED_VARIABLE",
)

import io.netifi.flatbuffers.plugin.tasks.FlatBuffers
import com.google.protobuf.gradle.*

plugins {
  `maven-publish`
  distribution
  signing
  kotlin("plugin.serialization")
  id("dev.elide.build.kotlin")
  alias(libs.plugins.protobuf)
  alias(libs.plugins.flatbuffers)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  val flat by creating {
    kotlin.srcDir("$projectDir/src/flat/kotlin")
    resources.srcDir("$projectDir/src/flat/resources")
  }

  val proto by creating {
    java.srcDirs(
      file("$buildDir/generated/source/proto/proto/java"),
    )
    kotlin.srcDirs(
      file("$projectDir/src/protobuf/kotlin"),
      file("$buildDir/generated/source/proto/proto/kotlin"),
    )
    resources.srcDir(
      "$projectDir/src/protobuf/resources"
    )
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }

  val kotlinx by creating {
    kotlin.srcDir("$projectDir/src/kotlinx/kotlin")
    resources.srcDir("$projectDir/src/kotlinx/resources")
  }

  val main by getting {
    // @TODO(sgammon): CI support for flatbuffers (specifically, flatc)
    // kotlin.srcDir("$buildDir/generated/source/flatbuffers")
  }
}

val compileOnly: Configuration by configurations.getting
val apiCommon: Configuration by configurations.creating
val implCommon: Configuration by configurations.creating {
  extendsFrom(apiCommon)
}
val protobufVariantCompileOnly: Configuration by configurations.creating {
  extendsFrom(compileOnly)
}
val flatImplementation: Configuration by configurations.getting {
  extendsFrom(implCommon)
}

configurations {
  create("modelInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(
      configurations["implementation"],
      configurations["kotlinxImplementation"],
    )
  }

  create("flatInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(
      configurations["flatImplementation"],
    )
  }
}

kotlin {
  target.compilations.all {
    kotlinOptions {
      jvmTarget = javaLanguageTarget
      javaParameters = true
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      allWarningsAsErrors = false
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(listOf(
        // do not warn for generated code
        "-nowarn"
      ))
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = false
    }
  }
}

flatbuffers {
  language = "kotlin"
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
  }
  generateProtoTasks {
    ofSourceSet("proto").forEach {
      it.builtins {
        id("kotlin")
      }
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

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  val compileFlatbuffers by creating(FlatBuffers::class) {
    description = "Generate Flatbuffers code for Kotlin/JVM"
    inputDir = file("${rootProject.projectDir}/proto")
    outputDir = file("$projectDir/src/main/flat")
  }

  val protobufJar by creating(Jar::class) {
    description = "Package a JAR of the Protocol Buffers implementation of Elide Proto"
    from(sourceSets.named("proto").get().output)
    archiveClassifier.set("protobuf")
  }

  val flatbuffersJar by creating(Jar::class) {
    description = "Package a JAR of the Flatbuffers implementation of Elide Proto"
    from(sourceSets.named("flat").get().output)
    archiveClassifier.set("flatbuffers")
  }

  val kotlinxJar by creating(Jar::class) {
    description = "Package a JAR of the KotlinX implementation of Elide Proto"
    from(sourceSets.named("kotlinx").get().output)
    archiveClassifier.set("kotlinx")
  }

  artifacts {
    archives(protobufJar)
    archives(flatbuffersJar)
    archives(kotlinxJar)
    archives(jar)
    add("modelInternal", protobufJar)
    add("modelInternal", kotlinxJar)
    add("flatInternal", flatbuffersJar)
  }
}

val protoImplementation: Configuration by configurations.getting {
  extendsFrom(implCommon)
}
val protoCompileOnly: Configuration by configurations.getting {
  extendsFrom(implCommon)
}
val kotlinxImplementation: Configuration by configurations.getting {
  extendsFrom(implCommon)
}
val kotlinxCompileOnly: Configuration by configurations.getting {
  extendsFrom(protoCompileOnly)
}

dependencies {
  // Common
  apiCommon(libs.kotlinx.datetime)
  implCommon(kotlin("stdlib"))
  implCommon(kotlin("stdlib-jdk8"))

  // Variant: Main
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  compileOnly(libs.protobuf.java)
  compileOnly(libs.protobuf.kotlin)
  compileOnly(libs.google.common.html.types.proto)
  compileOnly(libs.google.common.html.types.types)

  // Variant: Protobuf
  protoImplementation(kotlin("reflect"))
  protoImplementation(libs.protobuf.java)
  protoImplementation(libs.protobuf.kotlin)
  protoImplementation(libs.google.common.html.types.proto)
  protoImplementation(libs.google.common.html.types.types)
  protoCompileOnly(libs.google.cloud.nativeImageSupport)

  // Variant: Flatbuffers
  flatImplementation(kotlin("stdlib"))
  flatImplementation(kotlin("stdlib-jdk8"))
  flatImplementation(libs.flatbuffers.java.core)

  // Variant: KotlinX
  kotlinxImplementation(kotlin("reflect"))
  kotlinxImplementation(libs.protobuf.java)
  kotlinxImplementation(libs.protobuf.kotlin)
  kotlinxImplementation(libs.kotlinx.serialization.core.jvm)
  kotlinxImplementation(libs.kotlinx.serialization.json.jvm)
  kotlinxImplementation(libs.kotlinx.serialization.protobuf.jvm)
  kotlinxImplementation(tasks.named("protobufJar").get().outputs.files)
  kotlinxCompileOnly(libs.google.cloud.nativeImageSupport)
}
