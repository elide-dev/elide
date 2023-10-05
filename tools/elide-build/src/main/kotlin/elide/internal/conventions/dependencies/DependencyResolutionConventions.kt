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

package elide.internal.conventions.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.LockMode

/** Introduces dependency locking settings. */
internal fun Project.configureDependencyLocking() {
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

  tasks.register("resolveAndLockAll") {
    doFirst {
      require(gradle.startParameter.isWriteDependencyLocks)
    }

    doLast {
      // resolve all possible configurations
      configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
  }
}

/** Establishes the dependency conflict resolution policy. */
internal fun Project.configureDependencyResolution() {
  configurations.all {
    resolutionStrategy.apply {
      // fail eagerly on version conflict (includes transitive dependencies)
      failOnVersionConflict()

      // prefer modules that are part of this build
      preferProjectModules()
    }
  }
}
