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