@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  `maven-publish`
  distribution
  signing
  kotlin("plugin.serialization")
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

java {
  sourceCompatibility = JavaVersion.toVersion(javaLanguageTarget)
  targetCompatibility = JavaVersion.toVersion(javaLanguageTarget)
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(javaLanguageVersion))
  }

  sourceSets.all {
    languageSettings.apply {
      apiVersion = Elide.kotlinLanguage
      languageVersion = Elide.kotlinLanguage
      progressiveMode = true
      optIn("kotlin.ExperimentalUnsignedTypes")
    }
  }

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
    tasks.withType<KotlinCompile>().configureEach {
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

configureJava9ModuleInfo(multiRelease = true)

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

afterEvaluate {
  val compileTasks = listOf(
    "compileKotlinJava11",
    "compileKotlinJava17",
    "compileKotlinJava19",
  ).mapNotNull {
    try {
      tasks.named(it)
    } catch (err: Throwable) {
      // ignore
      null
    }
  }
  listOf(
    "apiBuild"
  ).forEach {
    try {
      tasks.named(it).configure {
        dependsOn(compileTasks)
      }
    } catch (e: Exception) {
      // ignore
    }
  }
}
