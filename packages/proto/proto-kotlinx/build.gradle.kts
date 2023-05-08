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
  kotlin("plugin.serialization")
  id("dev.elide.build.kotlin")
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.proto.language"] as String
val javaLanguageTarget = project.properties["versions.java.proto.target"] as String

sourceSets {
  /**
   * Variant: KotlinX
   */
  val main by getting
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
  test {
    useJUnitPlatform()
  }

  artifacts {
    archives(jar)
    add("modelInternal", jar)
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
    /** Publication: KotlinX */
    create<MavenPublication>("maven") {
      artifactId = artifactId.replace("proto-kotlinx", "elide-proto-kotlinx")
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      if (buildDocs) {
        artifact(javadocJar)
      }

      pom {
        name.set("Elide Protocol: KotlinX")
        description.set("Elide protocol implementation for KotlinX Serialization")
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
  // API
  api(libs.kotlinx.datetime)
  api(project(":packages:proto:proto-core"))
  api(libs.kotlinx.serialization.core.jvm)
  api(libs.kotlinx.serialization.json.jvm)
  api(libs.kotlinx.serialization.protobuf.jvm)

  // Implementation
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation(project(":packages:core"))
  implementation(project(":packages:base"))

  // Testing
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)
  testImplementation(project(":packages:test"))
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))
}
