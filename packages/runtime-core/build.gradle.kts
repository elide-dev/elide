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
)

import Java9Modularity.configure as configureJava9ModuleInfo

plugins {
  id("dev.elide.build.native.lib")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()
}

val initializeAtBuildTime = listOf(
  "kotlin.DeprecationLevel",
  "kotlin.annotation.AnnotationRetention",
  "kotlin.annotation.AnnotationTarget",
  "kotlin.coroutines.intrinsics.CoroutineSingletons",
)

val initializeAtBuildTimeTest = listOf(
  "org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter",
  "org.junit.platform.launcher.core.LauncherConfig",
)

val sharedLibArgs = listOf(
  "-H:+AuxiliaryEngineCache",
)

graalvmNative {
  testSupport = true

  agent {
    enabled = false
  }

  binaries {
    val mergedBuildArgs = initializeAtBuildTime.map { arg ->
      "--initialize-at-build-time=$arg"
    }.plus(sharedLibArgs)

    create("shared") {
      sharedLibrary = true
      buildArgs(mergedBuildArgs)
    }

    named("test") {
      fallback = false
      sharedLibrary = false
      quickBuild = true
      buildArgs(mergedBuildArgs)
    }
  }
}

dependencies {
  // Modules
  implementation(projects.packages.graalvm)

  // Kotlin / KotlinX
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)

  // compileOnly(libs.graalvm.sdk)
  // compileOnly(libs.graalvm.truffle.api)

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testCompileOnly(libs.graalvm.sdk)
}

configureJava9ModuleInfo(project)

tasks {
  test {
    maxHeapSize = "2G"
    maxParallelForks = 4
    environment("ELIDE_TEST", "true")
    systemProperty("elide.test", "true")
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
publishing.publications.withType<MavenPublication> {
  artifactId = artifactId.replace("runtime-core", "elide-runtime-core")
  if (buildDocs) {
    artifact(tasks.javadocJar)
  }

  pom {
    name = "Elide runtime core"
    url = "https://elide.dev"
    description = "Core API for the Elide polyglot runtime."

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

      developer {
        id = "darvld"
        name = "Dario Valdespino"
        email = "dvaldespino00@gmail.com"
      }
    }
    scm {
      url = "https://github.com/elide-dev/elide"
    }
  }
}

if (buildDocs) tasks.named("dokkaJavadoc").configure {
  dependsOn("kaptKotlin")
}
