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
  kotlin("plugin.serialization")
  id("org.graalvm.buildtools.native")
  id("dev.elide.build.jvm.kapt")
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

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
      quickBuild = quickbuild
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "--enable-all-security-services",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))

      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }

    named("optimized") {
      fallback = false
      quickBuild = quickbuild
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "-O2",
        "--enable-all-security-services",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))

      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }

    named("test") {
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "--enable-all-security-services",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))

      quickBuild = quickbuild
      javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of((project.properties["versions.java.language"] as String))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor = JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            })
          }
        }
      }
    }
  }
}
