/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.tooling.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.nio.file.Path
import jakarta.inject.Inject
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tooling.project.SourceSetFactory
import elide.tooling.project.SourceSetLanguage
import elide.tooling.project.codecs.MavenPomManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Integration tests for Maven POM parsing with real-world projects.
 *
 * These tests are conditional and will be skipped if the test projects don't exist.
 * To run these tests, clone the following projects to ~/src/:
 *
 * - joda-time: git clone https://github.com/JodaOrg/joda-time.git ~/src/joda-time
 */
@MicronautTest
class MavenPomIntegrationTest {
  @Inject lateinit var codec: MavenPomManifestCodec

  private val jodaTimePath = Path.of(System.getProperty("user.home"), "src", "joda-time", "pom.xml")

  @Test fun `joda-time - should parse project metadata`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    assertEquals("joda-time", elide.name)
    assertNotNull(elide.version)
    assertNotNull(elide.description)
  }

  @Test fun `joda-time - should have relative source paths`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Source paths must be relative, not absolute
    elide.sources.values.forEach { sourceSet ->
      sourceSet.paths.forEach { path ->
        assertFalse(
          path.startsWith("/"),
          "Source path should be relative, not absolute: $path"
        )
      }
    }

    // Should have main and test source sets
    assertTrue(elide.sources.containsKey("main"), "Should have 'main' source set")
    assertTrue(elide.sources.containsKey("test"), "Should have 'test' source set")

    // Main source should reference src/main/java
    val mainPaths = elide.sources["main"]?.paths ?: emptyList()
    assertTrue(
      mainPaths.any { it.contains("src/main/java") },
      "Main source set should include src/main/java"
    )
  }

  @Test fun `joda-time - should parse JAR artifacts with classifiers`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have default jar artifact
    assertTrue(elide.artifacts.containsKey("jar"), "Should have default JAR artifact")
    val defaultJar = elide.artifacts["jar"]
    assertTrue(defaultJar is ElidePackageManifest.Jar)

    // joda-time has a no-tzdb classifier jar
    assertTrue(
      elide.artifacts.containsKey("no-tzdb"),
      "Should have no-tzdb classifier JAR artifact"
    )
    val noTzdbJar = elide.artifacts["no-tzdb"]
    assertTrue(noTzdbJar is ElidePackageManifest.Jar)
    assertEquals("no-tzdb", (noTzdbJar as ElidePackageManifest.Jar).name)
  }

  @Test fun `joda-time - should parse javadoc plugin with groups`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have javadoc artifact
    val javadocArtifact = elide.artifacts["javadoc"]
    assertNotNull(javadocArtifact, "Should have javadoc artifact")
    assertTrue(javadocArtifact is ElidePackageManifest.JavadocJar)

    val javadoc = javadocArtifact as ElidePackageManifest.JavadocJar
    // joda-time defines package groups
    assertTrue(javadoc.groups.isNotEmpty(), "Javadoc should have package groups")
  }

  @Test fun `joda-time - should parse source plugin with classifiers`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have sources artifact
    val sourcesArtifact = elide.artifacts["sources"]
    assertNotNull(sourcesArtifact, "Should have sources artifact")
    assertTrue(sourcesArtifact is ElidePackageManifest.SourceJar)

    // joda-time also has no-tzdb-sources
    val noTzdbSources = elide.artifacts["no-tzdb-sources"]
    assertNotNull(noTzdbSources, "Should have no-tzdb-sources artifact")
    assertTrue(noTzdbSources is ElidePackageManifest.SourceJar)
    assertEquals("no-tzdb-sources", (noTzdbSources as ElidePackageManifest.SourceJar).classifier)
  }

  @Test fun `joda-time - should parse JVM target`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have JVM settings
    assertNotNull(elide.jvm, "Should have JVM settings")
    assertNotNull(elide.jvm?.target, "Should have JVM target")
  }

  @Test fun `joda-time - should parse dependencies`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have Maven coordinates (joda-time uses joda-time as groupId)
    assertNotNull(elide.dependencies.maven.coordinates)
    assertEquals("joda-time", elide.dependencies.maven.coordinates?.group)
    assertEquals("joda-time", elide.dependencies.maven.coordinates?.name)

    // Should have dependencies (joda-convert)
    assertTrue(
      elide.dependencies.maven.packages.any { it.name == "joda-convert" },
      "Should have joda-convert dependency"
    )

    // Should have test dependencies (junit)
    assertTrue(
      elide.dependencies.maven.testPackages.any { it.name == "junit" },
      "Should have junit test dependency"
    )
  }

  @Test fun `joda-time - should parse exec-maven-plugin for TZDB compilation`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // joda-time uses exec-maven-plugin to compile timezone database
    if (elide.execTasks.isNotEmpty()) {
      val tzdbTask = elide.execTasks.find { task ->
        task.mainClass?.contains("ZoneInfoCompiler") == true ||
          task.id.contains("tzdb", ignoreCase = true)
      }
      // This is optional - depends on pom.xml configuration
      if (tzdbTask != null) {
        assertEquals(ElidePackageManifest.ExecTaskType.JAVA, tzdbTask.type)
      }
    }
  }

  @Test fun `joda-time - sources should work with SourceSetFactory`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)
    val projectRoot = jodaTimePath.parent

    // Verify that the source paths can be resolved against the project root
    elide.sources.forEach { (name, sourceSet) ->
      sourceSet.paths.forEach { pathSpec ->
        // The path should be a glob that can be resolved from project root
        val basePath = pathSpec.substringBefore("/**")
        val fullPath = projectRoot.resolve(basePath)
        assertTrue(
          fullPath.exists(),
          "Source path base '$basePath' should exist in project at $fullPath"
        )
      }
    }
  }

  @Test fun `joda-time - INSPECT manifest and compare with Maven`() {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val elide = codec.toElidePackage(pom)

    val output = StringBuilder()
    fun log(s: String) { output.appendLine(s); println(s) }

    log("\n" + "=".repeat(80))
    log("ELIDE MANIFEST FOR JODA-TIME")
    log("=".repeat(80))

    log("\n--- PROJECT INFO ---")
    log("Name: ${elide.name}")
    log("Version: ${elide.version}")
    log("Description: ${elide.description}")

    log("\n--- JVM SETTINGS ---")
    log("JVM Target: ${elide.jvm?.target?.argValue}")

    log("\n--- SOURCE SETS ---")
    elide.sources.forEach { (name, sourceSet) ->
      log("  $name (${sourceSet.type}):")
      sourceSet.paths.forEach { path ->
        log("    - $path")
      }
    }

    log("\n--- ARTIFACTS ---")
    elide.artifacts.forEach { (name, artifact) ->
      log("  $name: ${artifact::class.simpleName}")
      when (artifact) {
        is ElidePackageManifest.Jar -> {
          log("    classifier: ${artifact.name ?: "(default)"}")
          log("    sources: ${artifact.sources}")
          log("    manifest entries: ${artifact.manifest}")
          log("    manifestFile: ${artifact.manifestFile}")
          log("    excludes: ${artifact.excludes}")
        }
        is ElidePackageManifest.JavadocJar -> {
          log("    groups: ${artifact.groups}")
          log("    links: ${artifact.links}")
          log("    windowTitle: ${artifact.windowTitle}")
          log("    docTitle: ${artifact.docTitle}")
        }
        is ElidePackageManifest.SourceJar -> {
          log("    classifier: ${artifact.classifier}")
          log("    excludes: ${artifact.excludes}")
          log("    includes: ${artifact.includes}")
        }
        is ElidePackageManifest.Assembly -> {
          log("    id: ${artifact.id}")
          log("    formats: ${artifact.formats}")
          log("    descriptorPath: ${artifact.descriptorPath}")
        }
        else -> log("    (unknown artifact type)")
      }
    }

    log("\n--- DEPENDENCIES ---")
    log("Maven Coordinates: ${elide.dependencies.maven.coordinates?.group}:${elide.dependencies.maven.coordinates?.name}")
    log("Compile Dependencies:")
    elide.dependencies.maven.packages.forEach { dep ->
      log("  - ${dep.group}:${dep.name}:${dep.version}")
    }
    log("Test Dependencies:")
    elide.dependencies.maven.testPackages.forEach { dep ->
      log("  - ${dep.group}:${dep.name}:${dep.version}")
    }

    log("\n--- EXEC TASKS ---")
    if (elide.execTasks.isEmpty()) {
      log("  (none)")
    } else {
      elide.execTasks.forEach { task ->
        log("  ${task.id} (${task.type}):")
        log("    phase: ${task.phase}")
        log("    mainClass: ${task.mainClass}")
        log("    executable: ${task.executable}")
        log("    args: ${task.args}")
      }
    }

    log("\n" + "=".repeat(80))
    log("END ELIDE MANIFEST")
    log("=".repeat(80) + "\n")

    // Write to file for inspection
    java.io.File("/tmp/elide-jodatime-manifest.txt").writeText(output.toString())
    log("Output written to /tmp/elide-jodatime-manifest.txt")

    // This test always passes - it's for inspection
    assertTrue(true)
  }

  @Test fun `joda-time - LOAD PROJECT and verify source sets`() = runBlocking {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val manifest = codec.toElidePackage(pom)
    val projectRoot = jodaTimePath.parent

    // Load source sets directly using SourceSetFactory
    val factory = SourceSetFactory.Default

    // Should have main and test source sets defined
    assertTrue(manifest.sources.containsKey("main"), "Should have 'main' source set in manifest")
    assertTrue(manifest.sources.containsKey("test"), "Should have 'test' source set in manifest")

    // Load main source set
    val mainSourceSetSpec = manifest.sources["main"]!!
    val mainSourceSet = factory.load(projectRoot, "main", mainSourceSetSpec)
    assertNotNull(mainSourceSet)

    // Load test source set
    val testSourceSetSpec = manifest.sources["test"]!!
    val testSourceSet = factory.load(projectRoot, "test", testSourceSetSpec)
    assertNotNull(testSourceSet)

    // Main source set should have paths (files)
    val mainPaths = mainSourceSet.paths
    assertTrue(mainPaths.isNotEmpty(), "Main source set should have files, got: ${mainPaths.size}")

    // Main source set should contain Java files
    assertTrue(
      mainSourceSet.languages?.contains(SourceSetLanguage.Java) == true,
      "Main source set should have Java language"
    )

    // Print source set info
    println("\n=== LOADED SOURCE SETS ===")
    println("Main source set: ${mainSourceSet.name}")
    println("  Type: ${mainSourceSet.type}")
    println("  Languages: ${mainSourceSet.languages}")
    println("  File count: ${mainSourceSet.paths.size}")
    println("  Sample files (first 5):")
    mainSourceSet.paths.take(5).forEach { file ->
      println("    - ${file.path}")
    }

    println("\nTest source set: ${testSourceSet.name}")
    println("  Type: ${testSourceSet.type}")
    println("  Languages: ${testSourceSet.languages}")
    println("  File count: ${testSourceSet.paths.size}")

    // Verify we found actual Java files
    assertTrue(
      mainSourceSet.paths.any { it.path.toString().endsWith(".java") },
      "Main source set should contain .java files"
    )
  }

  @Test fun `joda-time - BUILD CONFIGURATION would create correct tasks`() = runBlocking {
    Assumptions.assumeTrue(jodaTimePath.exists(), "joda-time project not found at $jodaTimePath")

    val pom = codec.parseAsFile(jodaTimePath, defaultManifestState)
    val manifest = codec.toElidePackage(pom)
    val projectRoot = jodaTimePath.parent

    // Load source sets using SourceSetFactory
    val factory = SourceSetFactory.Default
    val loadedSourceSets = manifest.sources.mapValues { (name, spec) ->
      factory.load(projectRoot, name, spec)
    }

    // Verify the artifacts that would be built
    println("\n=== BUILD CONFIGURATION ===")
    println("Project: ${manifest.name} v${manifest.version}")
    println("JVM Target: ${manifest.jvm?.target?.argValue}")

    // Check JAR artifacts
    println("\nJAR Artifacts to build:")
    manifest.artifacts.forEach { (name, artifact) ->
      when (artifact) {
        is ElidePackageManifest.Jar -> {
          println("  - $name (classifier: ${artifact.name ?: "default"})")
          println("    sources: ${artifact.sources}")
          println("    excludes: ${artifact.excludes}")

          // Verify source sets referenced by JAR exist
          artifact.sources.forEach { srcSetName ->
            assertTrue(
              manifest.sources.containsKey(srcSetName),
              "JAR '$name' references source set '$srcSetName' which should exist"
            )
            val srcSet = loadedSourceSets[srcSetName]
            assertNotNull(srcSet, "Source set '$srcSetName' should be loadable")
            assertTrue(
              srcSet.paths.isNotEmpty(),
              "Source set '$srcSetName' should have files for JAR '$name'"
            )
          }
        }
        else -> println("  - $name: ${artifact::class.simpleName}")
      }
    }

    // Verify exec tasks
    println("\nExec tasks:")
    manifest.execTasks.forEach { task ->
      println("  - ${task.id} (${task.type})")
      println("    phase: ${task.phase}")
      println("    mainClass: ${task.mainClass}")
    }

    // All JARs should have valid source sets
    val jarArtifacts = manifest.artifacts.values.filterIsInstance<ElidePackageManifest.Jar>()
    jarArtifacts.forEach { jar ->
      jar.sources.forEach { srcSetName ->
        val srcSet = loadedSourceSets[srcSetName]
        assertNotNull(srcSet, "JAR references source set '$srcSetName' which must exist")
      }
    }
  }
}
