/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

import org.jetbrains.kotlin.gradle.internal.KaptTask


plugins {
  java
  jacoco
  `jvm-test-suite`
  `maven-publish`

  kotlin("kapt")
  id("dev.elide.build.core")
  id("dev.elide.build.kotlin")
}

val defaultJavaVersion = "17"
val defaultKotlinVersion = "1.9"

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as? String ?: defaultJavaVersion
val ecmaVersion = project.properties["versions.ecma.language"] as String
val strictMode = project.properties["elide.strict"] as? String == "true"
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
  keepJavacAnnotationProcessors = true
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
