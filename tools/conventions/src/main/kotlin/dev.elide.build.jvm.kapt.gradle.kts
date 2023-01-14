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

  kotlin("kapt")
  id("dev.elide.build.core")
  id("dev.elide.build.kotlin")
}

val defaultJavaVersion = "11"
val defaultKotlinVersion = "1.8"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["strictMode"] as? String == "true"
val buildDocs = project.properties["buildDocs"] as String == "true"

// Compiler: Kotlin
// ----------------
// Override with JVM-specific (non-kapt) arguments.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageTarget
    javaParameters = true
    freeCompilerArgs = Elide.kaptCompilerArgs  // intentionally eliminates `-Xuse-K2`, which is unsupported by `kapt`
    allWarningsAsErrors = strictMode
    incremental = true
  }
}

// Compiler: `kapt`
// ----------------
// Configure Kotlin annotation processing.
kapt {
  useBuildCache = true
  includeCompileClasspath = false
  strictMode = true
  correctErrorTypes = true
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

// Compiler: Java
// --------------
// Configure Java compiler.
java {
  withSourcesJar()
  withJavadocJar()

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
  sourceCompatibility = javaLanguageTarget
  targetCompatibility = javaLanguageTarget
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

// Tasks: Javadoc Jar
// ------------------
// Build Javadocs from Dokka.
val javadocJar = tasks.named<Jar>("javadocJar") {
  if (buildDocs) {
    from(tasks.named("dokkaJavadoc"))
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
