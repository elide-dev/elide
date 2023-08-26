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
  "DSL_SCOPE_VIOLATION",
  "UNUSED_VARIABLE",
)

import io.netifi.flatbuffers.plugin.tasks.FlatBuffers

plugins {
  `maven-publish`
  distribution
  signing
  id("dev.elide.build.kotlin")
  id("dev.elide.build.jvm11")
  alias(libs.plugins.flatbuffers)
}

group = "dev.elide"
version = rootProject.version as String

val javaLanguageVersion = project.properties["versions.java.language"] as String
val javaLanguageTarget = project.properties["versions.java.target"] as String

sourceSets {
  /**
   * Variant: Flatbuffers
   */
  val main by getting
  val test by getting
}

configurations {
  // `flatInternal` uses the flatbuffers implementation only, rather than the full cruft of Protocol Buffers non-lite.
  create("flatInternal") {
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
      allWarningsAsErrors = true
      freeCompilerArgs = Elide.jvmCompilerArgsBeta.plus(listOf(
        // do not warn for generated code
        "-nowarn"
      ))
    }
  }

  // force -Werror to be off
  afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
      kotlinOptions.allWarningsAsErrors = true
    }
  }
}

flatbuffers {
  language = "kotlin"
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

  /**
   * Variant: Flatbuffers
   */
  val compileFlatbuffers by creating(FlatBuffers::class) {
    description = "Generate Flatbuffers code for Kotlin/JVM"
    inputDir = file("${rootProject.projectDir}/proto")
    outputDir = file("$projectDir/src/main/flat")
  }

  artifacts {
    archives(jar)
    add("flatInternal", jar)
  }

  val sourcesJar by registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier = "sources"
    from(sourceSets["main"].allSource)
  }
}

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
    /** Publication: Flatbuffers */
    create<MavenPublication>("maven") {
      artifactId = artifactId.replace("proto-flatbuffers", "elide-proto-flatbuffers")
      from(components["kotlin"])
      artifact(tasks["kotlinSourcesJar"])
      if (buildDocs) {
        artifact(javadocJar)
      }

      pom {
        name = "Elide Protocol: Flatbuffers"
        description = "Elide protocol implementation for Flatbuffers"
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

dependencies {
  // Common
  api(libs.kotlinx.datetime)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(projects.packages.core)
  implementation(projects.packages.base)
  testImplementation(projects.packages.test)
  testImplementation(libs.truth)
  testImplementation(libs.truth.java8)

  // Variant: Flatbuffers
  api(projects.packages.proto.protoCore)
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(libs.flatbuffers.java.core)
  testImplementation(project(":packages:proto:proto-core", configuration = "testBase"))
}

afterEvaluate {
  tasks.named("runKtlintCheckOverMainSourceSet").configure {
    enabled = false
  }
}
