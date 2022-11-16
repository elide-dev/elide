@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  java
  jacoco
  `jvm-test-suite`
  `maven-publish`

  id("dev.elide.build.core")
  id("dev.elide.build.kotlin")
}

val javaLanguageVersion = project.properties["versions.java.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["versions.java.language"] as String == "true"
val buildDocs = project.properties["buildDocs"] as String == "true"

// Compiler: Kotlin
// ----------------
// Override with JVM-specific (non-kapt) arguments.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageVersion
    javaParameters = true
    freeCompilerArgs = Elide.jvmCompilerArgs
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

// JVM: Testing
// ------------
// JVM test suite configuration.
testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

// Tasks: Javadoc Jar
// ------------------
// Build Javadocs from Dokka.
val javadocJar by tasks.creating(Jar::class) {
  archiveClassifier.set("javadoc")
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true

  if (buildDocs) {
    from(tasks.named("dokkaJavadoc"))
  }
}

// Compiler: Java
// --------------
// Configure Java compiler.
java {
  withSourcesJar()
  if (buildDocs) {
    withJavadocJar()
  }

  toolchain {
    languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
    vendor.set(JvmVendorSpec.GRAAL_VM)
    if (project.hasProperty("elide.graalvm.variant")) {
      val variant = project.property("elide.graalvm.variant") as String
      if (variant != "COMMUNITY") {
        vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
          "ENTERPRISE" -> "Oracle"
          else -> "GraalVM Community"
        }))
      }
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = javaLanguageVersion
  targetCompatibility = javaLanguageVersion
  options.isFork = true
  options.isIncremental = true
}

tasks.withType<Javadoc>().configureEach {
  isFailOnError = false
}

// Artifacts: Publishing
// ---------------------
// Settings for publishing library artifacts to Maven repositories.
publishing {
  publications.withType<MavenPublication> {
    artifact(tasks.named("sourcesJar"))
    if (buildDocs) {
      artifact(tasks.named("javadocJar"))
    }
  }
}

// Tasks: Binary Jar
// -----------------
// Configure manifest attributes present for all Elide libraries.
tasks.jar {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  manifest {
    attributes(mapOf(
      "Elide-Version" to Elide.version,
    ))
  }
}

// Tasks: Artifacts
// ----------------
// Mounts configured module artifacts.
tasks {
  artifacts {
    add("archives", tasks.named("sourcesJar"))
    if (buildDocs) {
      add("archives", javadocJar)
    }
  }
}

// Tasks: Jacoco Report
// --------------------
// Configures settings for the Jacoco reporting step.
tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    xml.required.set(true)
  }

  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "**/generated/**",
          "**/com/**",
          "**/grpc/gateway/**",
          "**/tools/elide/**",
        )
      }
    })
  )
}
