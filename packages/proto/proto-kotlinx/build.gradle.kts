@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions


plugins {
  `maven-publish`
  distribution
  signing
  kotlin("plugin.serialization")
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

configurations {
  // `modelInternalJvm` is the dependency used internally by other Elide packages to access the protocol model. at
  // present, the internal dependency uses the Protocol Buffers implementation, + the KotlinX tooling on top of that.
  create("modelInternalJvm") {
    isCanBeResolved = false
    isCanBeConsumed = true

    extendsFrom(configurations["jvmRuntimeClasspath"])
  }
}

kotlin {
  jvm {
    withJava()
  }

  sourceSets {
    /**
     * Variant: KotlinX
     */
    val jvmMain by getting {
      dependencies {
        // API
        api(libs.kotlinx.datetime)
        api(project(":packages:proto:proto-core"))
        implementation(libs.kotlinx.serialization.core.jvm)
        implementation(libs.kotlinx.serialization.protobuf.jvm)

        // Implementation
        implementation(kotlin("stdlib"))
        implementation(kotlin("stdlib-jdk8"))
        implementation(project(":packages:core"))
        runtimeOnly(kotlin("reflect"))
      }
    }
    val jvmTest by getting {
      dependencies {
        // Testing
        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(project(":packages:test"))
        implementation(project(":packages:proto:proto-core", configuration = "testBase"))
      }
    }
  }

  targets.all {
    compilations.all {
      kotlinOptions {
        apiVersion = Elide.kotlinLanguage
        languageVersion = Elide.kotlinLanguage
        allWarningsAsErrors = false

        if (this is KotlinJvmOptions) {
          jvmTarget = javaLanguageTarget
          javaParameters = true
        }
      }
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = false
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
  jvmTest {
    useJUnitPlatform()
  }

  artifacts {
    archives(jvmJar)
    add("modelInternalJvm", jvmJar)
  }
}

val sourcesJar by tasks.getting(org.gradle.jvm.tasks.Jar::class)

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

publishing {
  publications {
    /** Publication: KotlinX */
    create<MavenPublication>("maven") {
      artifactId = artifactId.replace("proto-kotlinx", "elide-proto-kotlinx")
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      if (buildDocs) {
        artifact(javadocJar)
      }

      pom {
        name = "Elide Protocol: KotlinX"
        description = "Elide protocol implementation for KotlinX Serialization"
        url = "https://elide.dev"
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
}
