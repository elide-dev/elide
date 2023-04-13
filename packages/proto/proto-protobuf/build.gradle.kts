@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION", "UNUSED_VARIABLE",
)

import com.google.protobuf.gradle.*

plugins {
  `maven-publish`
  distribution
  signing
  id("dev.elide.build.kotlin")
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  /**
   * Variant: Protocol Buffers
   */
  val main by getting {
    proto {
      srcDir("${rootProject.projectDir}/proto")
    }
  }
  val test by getting
}

configurations {
  // `modelInternal` is the dependency used internally by other Elide packages to access the protocol model. at present,
  // the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternal") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["implementation"])
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

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
}

tasks {
  test {
    useJUnitPlatform()
  }

  artifacts {
    archives(jar)
    add("modelInternal", jar)
  }
}

publishing {
  publications {
    /** Publication: Protocol Buffers */
    create<MavenPublication>("maven") {
      artifactId = artifactId.replace("proto-protobuf", "elide-proto-protobuf")
      from(components["kotlin"])

      pom {
        name.set("Elide Protocol: Protobuf")
        description.set("Elide protocol implementation for Protocol Buffers")
      }
    }
  }
}

dependencies {
  // API
  api(libs.kotlinx.datetime)
  api(project(":packages:proto:proto-core"))
  api(libs.protobuf.java)
  api(libs.protobuf.util)
  api(libs.protobuf.kotlin)

  // Implementation
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  implementation(libs.google.common.html.types.proto)
  implementation(libs.google.common.html.types.types)

  // Compile-only
  compileOnly(libs.google.cloud.nativeImageSupport)

  // Test
  testImplementation(project(":packages:test"))
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(libs.truth.proto)
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))
}
