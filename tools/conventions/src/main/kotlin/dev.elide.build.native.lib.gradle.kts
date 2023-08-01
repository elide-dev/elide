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

@file:Suppress("UnstableApiUsage")

plugins {
  publishing
  signing
  `java-library`
  kotlin("plugin.serialization")
  id("org.graalvm.buildtools.native")
  id("dev.elide.build.jvm.kapt")
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

tasks.named<Jar>("jar") {
  from("collectReachabilityMetadata")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}

graalvmNative {
  testSupport = true

  metadataRepository {
    enabled = true
    version = GraalVMVersions.graalvmMetadata
  }

  agent {
    defaultMode = "standard"
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    enableExperimentalPredefinedClasses = false
    enableExperimentalUnsafeAllocationTracing = false
    trackReflectionMetadata = true
    enabled = true

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  binaries {
    named("main") {
      fallback = false
      sharedLibrary = true
      quickBuild = quickbuild
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))
    }

    named("test") {
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))

      quickBuild = quickbuild
    }
  }
}
