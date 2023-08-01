@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

plugins {
  `maven-publish`
  distribution
  signing
  id("dev.elide.build.multiplatform")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

kotlin {
  jvm {
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        // Common
        api(libs.kotlinx.datetime)
        implementation(kotlin("stdlib"))
        implementation(project(":packages:core"))
        implementation(project(":packages:base"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(project(":packages:test"))
      }
    }

    /**
     * Variant: Core
     */
    val jvmMain by getting {
      dependencies {
        // Common
        implementation(kotlin("stdlib-jdk8"))
      }
    }
    val jvmTest by getting {
      dependencies {
        // Common
        implementation(libs.truth)
        implementation(libs.truth.java8)
        implementation(libs.junit.jupiter.api)
        implementation(libs.junit.jupiter.params)
        runtimeOnly(libs.junit.jupiter.engine)
      }
    }
  }
}

// Configurations: Testing
val testBase: Configuration by configurations.creating {}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
  options.isFork = true
  options.isIncremental = true
  options.isWarnings = false
  options.compilerArgs.add("-Xlint:-deprecation")
}

tasks {
  jvmTest {
    useJUnitPlatform()
  }

  /**
   * Variant: Core
   */
  val testJar by registering(Jar::class) {
    description = "Base (abstract) test classes for all implementations"
    archiveClassifier = "tests"
    from(sourceSets.named("test").get().output)
  }

  artifacts {
    archives(jvmJar)
    add("testBase", testJar)
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
    /** Publication: Core */
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      if (buildDocs) {
        artifact(javadocJar)
      }

      artifactId = artifactId.replace("proto-core", "elide-proto-core")

      pom {
        name = "Elide Protocol: API"
        description = "API headers and services for the Elide Protocol"
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
