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

plugins {
  `maven-publish`
  distribution
  signing
  idea

  id("com.adarshr.test-logger")
  id("com.github.ben-manes.versions")
}


// Plugin: Test Logger
// -------------------
// Configure test logging.
testlogger {
  theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
  showExceptions = System.getenv("TEST_EXCEPTIONS") == "true"
  showFailed = true
  showPassed = true
  showSkipped = true
  showFailedStandardStreams = true
  showFullStackTraces = true
  slowThreshold = 30000L
}

// Tasks: Test
// -----------
// Settings for testsuite execution and test retries.
tasks.withType<Test>().configureEach {
  maxParallelForks = 4
}

// Tasks: Tar
// ----------
// Configure tasks which produce tarballs (improves caching/hermeticity).
tasks.withType<Jar>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
  isZip64 = true
}

// Tasks: Zip
// ----------
// Configure tasks which produce zip archives (improves caching/hermeticity).
tasks.withType<Zip>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
  isZip64 = true
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
