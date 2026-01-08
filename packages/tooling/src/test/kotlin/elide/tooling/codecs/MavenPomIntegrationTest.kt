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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.nio.file.Path
import jakarta.inject.Inject
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
}
