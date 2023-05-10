@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION", "UNUSED_VARIABLE",
)

import ElideTargetSuite.configureMultiReleaseJar
import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  `maven-publish`
  distribution
  signing
  alias(libs.plugins.protobuf)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.proto.language"] as String
val javaLanguageTarget = project.properties["versions.java.proto.target"] as String

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
    /** Publication: Protocol Buffers */
    create<MavenPublication>("maven") {
      artifactId = artifactId.replace("proto-protobuf", "elide-proto-protobuf")
      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      if (buildDocs) {
        artifact(javadocJar)
      }

      pom {
        name.set("Elide Protocol: Protobuf")
        description.set("Elide protocol implementation for Protocol Buffers")
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

configureMultiReleaseJar()

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

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = "11"
    javaParameters = true
    freeCompilerArgs = Elide.kaptCompilerArgs
    allWarningsAsErrors = true
    incremental = true
  }
}

afterEvaluate {
  val compileTasks = listOf(
    "compileKotlinJava11",
    "compileKotlinJava17",
    "compileKotlinJava19",
  )
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
