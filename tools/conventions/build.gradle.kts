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
  "DSL_SCOPE_VIOLATION",
  "UnstableApiUsage",
)

import java.net.URI

plugins {
  `maven-publish`
  distribution
  signing
  idea

  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

group = "dev.elide.tools"
version = rootProject.version as String

val kotlinVersion by project.properties
val enableAtomicfu = project.properties["elide.atomicFu"] == "true"

repositories {
  maven("https://maven.pkg.st/")
  maven("https://gradle.pkg.st/")
}

dependencies {
  implementation(gradleApi())
  implementation(libs.plugin.buildConfig)
  implementation(libs.plugin.graalvm)
  implementation(libs.plugin.docker)
  implementation(libs.plugin.detekt)
  implementation(libs.plugin.dokka)
  implementation(libs.plugin.kover)
  implementation(libs.plugin.micronaut)
  implementation(libs.plugin.sonar)
  implementation(libs.plugin.spotless)
  implementation(libs.plugin.testLogger)
  implementation(libs.plugin.versionCheck)
  implementation(embeddedKotlin("allopen"))
  implementation(embeddedKotlin("noarg"))
  implementation(embeddedKotlin("serialization"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion") {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-sam-with-receiver")
  }
  implementation(libs.plugin.kotlin.samWithReceiver)

  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  if (enableAtomicfu) {
    implementation(libs.plugin.kotlinx.atomicfu)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

afterEvaluate {
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
      apiVersion = "1.9"
      languageVersion = "1.9"
      jvmTarget = "17"
      javaParameters = true
      allWarningsAsErrors = true
      incremental = true
    }
  }
}


// Dependencies: Locking
// ---------------------
// Produces sealed dependency locks for each module.
dependencyLocking {
  lockMode = LockMode.LENIENT
  ignoredDependencies.addAll(listOf(
    "org.jetbrains.kotlinx:atomicfu*",
    "org.jetbrains.kotlinx:kotlinx-serialization*",
  ))
}

tasks.register("resolveAndLockAll") {
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks)
  }
  doLast {
    configurations.filter {
      // Add any custom filtering on the configurations to be resolved
      it.isCanBeResolved
    }.forEach { it.resolve() }
  }
}

// Dependencies: Conflicts
// -----------------------
// Establishes a strict conflict policy for dependencies.
configurations.all {
  resolutionStrategy {
    // fail eagerly on version conflict (includes transitive dependencies)
    failOnVersionConflict()

    // prefer modules that are part of this build
    preferProjectModules()
  }
}

// Artifacts: Signing
// ------------------
// If so directed, make sure to sign outgoing artifacts.
signing {
  if (project.hasProperty("enableSigning") && project.properties["enableSigning"] == "true") {
    sign(configurations.archives.get())
    sign(publishing.publications)
  }
}

// Artifacts: Publishing
// ---------------------
// Settings for publishing library artifacts to Maven repositories.
publishing {
  repositories {
    maven {
      name = "elide"
      url = URI.create(project.properties["elide.publish.repo.maven"] as String)

      if (project.hasProperty("elide.publish.repo.maven.auth")) {
        credentials {
          username = (project.properties["elide.publish.repo.maven.username"] as? String
            ?: System.getenv("PUBLISH_USER"))?.ifBlank { null }
          password = (project.properties["elide.publish.repo.maven.password"] as? String
            ?: System.getenv("PUBLISH_TOKEN"))?.ifBlank { null }
        }
      }
    }
  }

  publications.withType<MavenPublication> {
    pom {
      name = "Elide Tools: Conventions"
      description = "Gradle convention plugins for use with Elide."
      url = "https://github.com/elide-dev/v3"

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/v3/blob/v3/LICENSE"
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
        url = "https://github.com/elide-dev/v3"
      }
    }
  }
}
