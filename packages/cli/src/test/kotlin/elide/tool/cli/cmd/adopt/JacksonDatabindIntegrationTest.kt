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

package elide.tool.cli.cmd.adopt

import kotlin.test.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Integration test for Jackson Databind - validates complex parent POM hierarchy resolution.
 *
 * Jackson Databind has a multi-level parent hierarchy:
 * jackson-databind -> jackson-base -> jackson-parent -> oss-parent
 *
 * This test uses the cloned Jackson Databind repository at /tmp/jackson-databind or
 * /private/tmp/jackson-databind.
 * If the repository doesn't exist, the test is skipped.
 */
class JacksonDatabindIntegrationTest {
  private val jacksonDatabindPath = findJacksonDatabindPath()

  private fun findJacksonDatabindPath(): Path? {
    val candidates = listOf(
      Paths.get("/tmp/jackson-databind/pom.xml"),
      Paths.get("/private/tmp/jackson-databind/pom.xml")
    )
    return candidates.firstOrNull { it.exists() }
  }

  @BeforeTest
  fun checkPreconditions() {
    if (jacksonDatabindPath == null) {
      println("Skipping Jackson Databind integration test - repository not found")
      println("To enable this test, clone: git clone --depth 1 https://github.com/FasterXML/jackson-databind.git /tmp/jackson-databind")
    }
  }

  @Test
  fun testParseJacksonDatabind() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)

    // Verify basic project info
    assertEquals("com.fasterxml.jackson.core", pom.groupId)
    assertEquals("jackson-databind", pom.artifactId)
    assertTrue(pom.version.startsWith("2."), "Version should be 2.x: ${pom.version}")
    assertEquals("jackson-databind", pom.name)
    assertNotNull(pom.description)

    // Verify parent POM (jackson-base)
    assertNotNull(pom.parent, "Should have parent POM")
    assertEquals("com.fasterxml.jackson", pom.parent!!.groupId)
    assertEquals("jackson-base", pom.parent!!.artifactId)
    assertNotNull(pom.parent!!.version)

    // Verify properties are loaded (including from parent chain)
    assertTrue(pom.properties.isNotEmpty(), "Should have properties")

    // Jackson uses OSGi bundle configuration
    assertTrue(
      pom.properties.containsKey("osgi.export") ||
      pom.properties.containsKey("maven.compiler.source"),
      "Should have either OSGi or compiler properties"
    )

    // Verify dependencies
    assertTrue(pom.dependencies.isNotEmpty(), "Should have dependencies")

    // Should have jackson-annotations dependency
    val annotationsDep = pom.dependencies.find { it.artifactId == "jackson-annotations" }
    assertNotNull(annotationsDep, "Should have jackson-annotations dependency")
    assertEquals("com.fasterxml.jackson.core", annotationsDep.groupId)

    // Should have jackson-core dependency
    val coreDep = pom.dependencies.find { it.artifactId == "jackson-core" }
    assertNotNull(coreDep, "Should have jackson-core dependency")
    assertEquals("com.fasterxml.jackson.core", coreDep.groupId)
  }

  @Test
  fun testMultiLevelParentResolution() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)

    // Jackson Databind has multi-level parent hierarchy:
    // jackson-databind -> jackson-base -> jackson-parent -> oss-parent

    // Parent should be jackson-base
    assertNotNull(pom.parent, "Should have direct parent")
    assertEquals("jackson-base", pom.parent!!.artifactId)

    // Properties from the entire parent chain should be inherited
    assertTrue(pom.properties.isNotEmpty(), "Should inherit properties from parent chain")

    // Maven compiler properties should be inherited from parent chain
    assertTrue(
      pom.properties.containsKey("maven.compiler.source") ||
      pom.properties.containsKey("java.version"),
      "Should have compiler configuration from parent chain"
    )

    // dependencyManagement from parent chain should be available
    // Jackson parent defines versions for jackson components
    assertTrue(pom.dependencyManagement.isNotEmpty(), "Should have dependencyManagement from parents")

    // Version should be consistent (likely defined in parent)
    assertTrue(pom.version.isNotEmpty(), "Should have version (from self or parent)")
  }

  @Test
  fun testJacksonDependencyManagement() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)

    // Jackson uses dependency management extensively across modules
    assertTrue(pom.dependencyManagement.isNotEmpty(), "Should have dependency management")

    // Core jackson dependencies should have coordinated versions
    val deps = pom.dependencies

    // Find jackson-core and jackson-annotations
    val coreDep = deps.find { it.artifactId == "jackson-core" }
    val annotationsDep = deps.find { it.artifactId == "jackson-annotations" }

    if (coreDep != null && annotationsDep != null) {
      // Versions should be defined (either explicitly or from dependencyManagement)
      assertTrue(
        coreDep.version?.isNotEmpty() == true ||
        pom.dependencyManagement.containsKey("com.fasterxml.jackson.core:jackson-core"),
        "jackson-core should have version management"
      )

      assertTrue(
        annotationsDep.version?.isNotEmpty() == true ||
        pom.dependencyManagement.containsKey("com.fasterxml.jackson.core:jackson-annotations"),
        "jackson-annotations should have version management"
      )
    }
  }

  @Test
  fun testGeneratePklForJackson() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)
    val pkl = PklGenerator.generate(pom)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""), "Should amend elide:project.pkl")
    assertTrue(pkl.contains("name = \"jackson-databind\""), "Should have correct project name")
    assertTrue(pkl.contains("description = "), "Should have description")

    // Verify Maven Central is included
    assertTrue(
      pkl.contains("[\"central\"] = \"https://repo.maven.apache.org/maven2\""),
      "Should include Maven Central"
    )

    // Verify dependencies section
    assertTrue(pkl.contains("packages {"), "Should have packages section")

    // Jackson modules should be in packages (not test scope)
    val hasJacksonCore = pkl.contains("jackson-core")
    val hasJacksonAnnotations = pkl.contains("jackson-annotations")

    assertTrue(
      hasJacksonCore || hasJacksonAnnotations,
      "Should include jackson dependencies"
    )

    // Basic PKL syntax validation
    assertTrue(pkl.contains("{"), "Should have opening braces")
    assertTrue(pkl.contains("}"), "Should have closing braces")

    // Should have proper structure
    val braceCount = pkl.count { it == '{' }
    val closeBraceCount = pkl.count { it == '}' }
    assertEquals(braceCount, closeBraceCount, "Braces should be balanced")
  }

  @Test
  fun testBuildPluginsDetection() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)

    // Jackson uses various Maven plugins
    assertTrue(pom.plugins.isNotEmpty(), "Should detect build plugins")

    val pluginIds = pom.plugins.map { "${it.groupId}:${it.artifactId}" }

    // Common plugins in Jackson projects
    val hasCommonPlugins = pluginIds.any {
      it.contains("maven-compiler-plugin") ||
      it.contains("maven-surefire-plugin") ||
      it.contains("maven-bundle-plugin") || // OSGi bundles
      it.contains("maven-jar-plugin")
    }

    assertTrue(hasCommonPlugins, "Should detect at least one common Maven plugin")

    // Verify default groupId handling
    pom.plugins.forEach { plugin ->
      assertTrue(
        plugin.groupId.isNotEmpty(),
        "Plugin ${plugin.artifactId} should have groupId (explicit or default)"
      )
    }
  }

  @Test
  fun testPropertyInterpolation() {
    // Skip if repo not cloned
    if (jacksonDatabindPath == null) {
      return
    }

    val pom = PomParser.parse(jacksonDatabindPath!!)

    // Jackson uses property-based version management
    assertTrue(pom.properties.isNotEmpty(), "Should have properties")

    // Version should be properly resolved
    assertTrue(pom.version.isNotEmpty(), "Version should be resolved")
    assertFalse(pom.version.contains("\${"), "Version should not contain unresolved properties")

    // Dependency versions should be resolved
    pom.dependencies.forEach { dep ->
      if (dep.version != null) {
        assertFalse(
          dep.version!!.contains("\${") && !dep.version!!.contains(":"),
          "Dependency ${dep.artifactId} version should be resolved or have defaults"
        )
      }
    }
  }
}
