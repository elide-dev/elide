/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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
  id("org.jetbrains.dokka")
}

val defaultKotlinVersion = "2.0"

val strictMode = project.properties["strictMode"] as? String == "true"
val enableK2 = project.properties["elide.kotlin.k2"] as? String == "true"
val javaLanguageVersion = project.properties["versions.java.language"] as? String ?: error(
  "Specify Java version at `versions.java.language`"
)
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: error(
  "Specify Java target at `versions.java.target`"
)
val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as? String ?: defaultKotlinVersion
val buildDocs = (project.properties["buildDocs"] as? String ?: "true") == "true"

// Compiler: Kotlin
// ----------------
// Override with JVM-specific (non-kapt) arguments.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    apiVersion = Elide.kotlinLanguage
    languageVersion = Elide.kotlinLanguage
    jvmTarget = javaLanguageTarget
    javaParameters = true
    freeCompilerArgs = freeCompilerArgs.plus(Elide.jvmCompilerArgs).toSortedSet().toList()
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
  archiveClassifier = "javadoc"
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

tasks.withType<Jar>().configureEach {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
    xml.required = true
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
