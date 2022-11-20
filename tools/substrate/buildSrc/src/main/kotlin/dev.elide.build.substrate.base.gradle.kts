plugins {
  `maven-publish`
  distribution
  signing
  idea
  kotlin("jvm")
}


// Dependencies: Locking
// ---------------------
// Produces sealed dependency locks for each module.
dependencyLocking {
  ignoredDependencies.addAll(
    listOf(
      "org.jetbrains.kotlinx:atomicfu*",
      "org.jetbrains.kotlinx:kotlinx-serialization*",
    )
  )
}

// Artifacts: Signing
// ------------------
// If so directed, make sure to sign outgoing artifacts.
signing {
  sign(configurations.archives.get())
  sign(publishing.publications)
}
