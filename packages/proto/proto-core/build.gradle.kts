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
}

publishing {
  publications {
    /** Publication: Core */
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      artifactId = artifactId.replace("proto-core", "elide-proto-core")

      pom {
        name.set("Elide Protocol: API")
        description.set("API headers and services for the Elide Protocol")
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
