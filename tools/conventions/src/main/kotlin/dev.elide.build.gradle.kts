
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
// Settings for testsuite execution.
tasks.withType<Test>().configureEach {
  maxParallelForks = 4
}

// Tasks: Tar
// ----------
// Configure tasks which produce tarballs.
tasks.withType<Jar>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}

// Tasks: Zip
// ----------
// Configure tasks which produce zip archives.
tasks.withType<Zip>().configureEach {
  isReproducibleFileOrder = true
  isPreserveFileTimestamps = false
}


// Dependencies: Locking
// ---------------------
// Produces sealed dependency locks for each module.
dependencyLocking {
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

    // fail if non-reproducible
    failOnNonReproducibleResolution()

    // prefer modules that are part of this build
    preferProjectModules()

    if (name.contains("detached")) {
      disableDependencyVerification()
    }
  }
}
