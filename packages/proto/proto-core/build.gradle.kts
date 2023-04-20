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
  id("dev.elide.build.kotlin")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  /**
   * Variant: Core
   */
  val main by getting
  val test by getting
}

// Configurations: Testing
val testBase: Configuration by configurations.creating {}

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

  /**
   * Variant: Core
   */
  val testJar by creating(Jar::class) {
    description = "Base (abstract) test classes for all implementations"
    archiveClassifier.set("tests")
    from(sourceSets.named("test").get().output)
  }

  artifacts {
    archives(jar)
    add("testBase", testJar)
  }

  val sourcesJar by registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
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
        name.set("Elide Protocol: API")
        description.set("API headers and services for the Elide Protocol")
        url.set("https://elide.dev")
        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
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
          url.set("https://github.com/elide-dev/elide")
        }
      }
    }
  }
}

dependencies {
  // Common
  api(libs.kotlinx.datetime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(project(":packages:core"))
  implementation(project(":packages:base"))
  testImplementation(project(":packages:test"))
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
}
