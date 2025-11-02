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

package elide.internal.conventions.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.kotlin.dsl.exclude
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.ElideBuildExtension

// Determine whether enterprise features are enabled.
private val isEnterprise: Project.() -> Boolean = {
  findProperty("elide.graalvm.variant") == "ENTERPRISE"
}

// Truffle Enterprise artifacts which must be omitted when building against GraalVM CE.
private val nonEnterpriseExclusions = listOf(
  "org.graalvm.truffle" to "truffle-enterprise",
  "org.graalvm.llvm" to "llvm-language-enterprise",
  "org.graalvm.llvm" to "llvm-language-native-enterprise",
  "org.graalvm.python" to "python-language-enterprise",
)

// Configurations which are exempt from locking checks.
private val lockingExemptConfigurations = sortedSetOf(
  "nativeMainImplementationDependenciesMetadata",
  "nativeTestImplementationDependenciesMetadata",
)

// Netty dependencies which are exempt from version pins.
private val nettyExemptions = sortedSetOf(
  "tcnative",
  "boringssl",
)

// Configurations from which to omit distributed GraalVM modules.
private val gvmConfigurations = sortedSetOf(
  "compileClasspath",
  "runtimeClasspath",
)

// Whether we are currently running under GraalVM.
private val isGraalVm: Boolean = (
  (System.getProperty("java.vm.version") ?: "").let { version ->
    "jvmci" in version
  }
)

/** Introduces dependency locking settings. */
internal fun Project.configureDependencyLocking(conventions: ElideBuildExtension) {
  if (conventions.deps.locking) {
    // configure dependency locking
    dependencyLocking {
      lockMode.set(LockMode.LENIENT)
      ignoredDependencies.addAll(
        listOf(
          "org.jetbrains.kotlinx:atomicfu*",
          "org.jetbrains.kotlinx:kotlinx-serialization*",
        ),
      )
    }
  }

  tasks.register("resolveAndLockAll") {
    doFirst {
      require(gradle.startParameter.isWriteDependencyLocks) {
        "Please pass `--write-locks` to resolve and lock dependencies"
      }
      require(findProperty("elide.lockDeps") == "true") {
        "Please set `elide.lockDeps=true` to resolve and lock dependencies"
      }
    }

    doLast {
      // resolve all possible configurations
      configurations.filter {
        it.isCanBeResolved && !lockingExemptConfigurations.contains(it.name) && !it.name.lowercase().let { name ->
          name.contains("sources") || name.contains("documentation")
        }
      }.forEach { it.resolve() }
    }
  }
}

/** Establishes the dependency conflict resolution policy. */
internal fun Project.configureDependencyResolution(conventions: ElideBuildExtension) {
  configurations.all {
    resolutionStrategy.apply {
      // prefer modules that are part of this build
      preferProjectModules()

      if (conventions.deps.strict) {
        // fail eagerly on version conflict (includes transitive dependencies)
        failOnVersionConflict()
      }
    }

    resolutionStrategy.eachDependency {
      // process dependency pins: kotlin
      if (requested.group == "org.jetbrains.kotlin" && requested.name.contains("stdlib")) {
        useVersion(Versions.KOTLIN_SDK_PIN)
        because("pin kotlin stdlib")
      }

      if (requested.group == "org.jetbrains.kotlin" && requested.name.contains("embedded")) {
        useVersion(Versions.KOTLIN_SDK_PIN)
        because("pin kotlin compiler")
      }

      // process dependency pins: netty
      if (requested.group == "io.netty" && !nettyExemptions.any { requested.name.contains(it) }) {
        useVersion(Versions.NETTY)
        because("pin netty")
      }

      // process dependency pins: bouncycastle
      if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk18on") {
        useVersion(Versions.BOUNCYCASTLE)
        because("pin bouncycastle")
      }

      // process dependency pins: guava
      if (requested.group == "com.google.guava" && requested.name.contains("guava")) {
        useVersion(Versions.GUAVA)
        because("pin guava")
      }

      // process dependency pins: grpc
      if (requested.group == "io.grpc" && !requested.name.contains("kotlin")) {
        useVersion(Versions.GRPC)
        because("pin grpc")
      }

      // process dependency pins: jline
      if (requested.group == "org.jline") {
        useVersion(Versions.JLINE)
        because("pin jline")
      }

      // process dependency pins: okio
      if (requested.group == "com.squareup.okio") {
        useVersion(Versions.OKIO)
        because("pin okio")
      }

      // process dependency pins: graalvm
      if (requested.group.contains("org.graalvm") && !requested.group.contains("buildtools")) {
        if (requested.group.contains("ruby") || requested.name.contains("ruby")) {
          useVersion(Versions.GRAALVM_RUBY)
        } else {
          useVersion(Versions.GRAALVM)
        }
        because("pin graalvm")
      }
    }

    if (isGraalVm && !isEnterprise.invoke(this@configureDependencyResolution)) nonEnterpriseExclusions.forEach {
      if (gvmConfigurations.contains(this@all.name)) {
        exclude(group = it.first, module = it.second)
      }
    }
  }
}
