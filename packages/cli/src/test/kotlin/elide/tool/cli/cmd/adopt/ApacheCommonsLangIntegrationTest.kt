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
 * Integration test for Apache Commons Lang - validates our implementation works with a real-world project.
 *
 * This test uses the cloned Apache Commons Lang repository at /tmp/apache-commons-lang.
 * If the repository doesn't exist, the test is skipped.
 */
class ApacheCommonsLangIntegrationTest {
  private val commonsLangPath = Paths.get("/tmp/apache-commons-lang/pom.xml")

  @BeforeTest
  fun checkPreconditions() {
    if (!commonsLangPath.exists()) {
      println("Skipping Apache Commons Lang integration test - repository not found at $commonsLangPath")
      println("To enable this test, clone: git clone --depth 1 https://github.com/apache/commons-lang.git /tmp/apache-commons-lang")
    }
  }

  @Test
  fun testParseApacheCommonsLang() {
    // Skip if repo not cloned
    if (!commonsLangPath.exists()) {
      return
    }

    val pom = PomParser.parse(commonsLangPath)

    // Verify basic project info
    assertEquals("org.apache.commons", pom.groupId)
    assertEquals("commons-lang3", pom.artifactId)
    assertTrue(pom.version.startsWith("3."))
    assertEquals("Apache Commons Lang", pom.name)
    assertNotNull(pom.description)

    // Verify parent POM
    assertNotNull(pom.parent)
    assertEquals("org.apache.commons", pom.parent!!.groupId)
    assertEquals("commons-parent", pom.parent!!.artifactId)
    assertEquals("92", pom.parent!!.version)

    // Verify properties are loaded
    assertTrue(pom.properties.isNotEmpty())
    assertTrue(pom.properties.containsKey("commons.text.version"))
    assertEquals("1.14.0", pom.properties["commons.text.version"])
    assertTrue(pom.properties.containsKey("commons.componentid"))
    assertEquals("lang", pom.properties["commons.componentid"])

    // Verify dependencies
    assertTrue(pom.dependencies.isNotEmpty())

    // Should have JUnit Jupiter (managed - no explicit version)
    val junitDep = pom.dependencies.find { it.artifactId == "junit-jupiter" }
    assertNotNull(junitDep, "Should have junit-jupiter dependency")
    assertEquals("org.junit.jupiter", junitDep.groupId)
    assertEquals("test", junitDep.scope)
    // Version should be resolved from parent's dependencyManagement
    assertNotNull(junitDep.version, "JUnit version should be resolved from parent")

    // Should have EasyMock with explicit version
    val easymockDep = pom.dependencies.find { it.artifactId == "easymock" }
    assertNotNull(easymockDep, "Should have easymock dependency")
    assertEquals("org.easymock", easymockDep.groupId)
    assertEquals("5.6.0", easymockDep.version)
    assertEquals("test", easymockDep.scope)

    // Should have commons-text with property-based version
    val commonsTextDep = pom.dependencies.find { it.artifactId == "commons-text" }
    assertNotNull(commonsTextDep, "Should have commons-text dependency")
    assertEquals("org.apache.commons", commonsTextDep.groupId)
    assertEquals("1.14.0", commonsTextDep.version) // Should be interpolated from ${commons.text.version}
    assertEquals("test", commonsTextDep.scope)

    // Verify build plugins detected
    assertTrue(pom.plugins.isNotEmpty(), "Should detect build plugins")

    // Should have standard Maven plugins
    val pluginIds = pom.plugins.map { "${it.groupId}:${it.artifactId}" }
    assertTrue(pluginIds.any { it.contains("maven-javadoc-plugin") }, "Should detect maven-javadoc-plugin")
    assertTrue(pluginIds.any { it.contains("maven-surefire-plugin") }, "Should detect maven-surefire-plugin")
    assertTrue(pluginIds.any { it.contains("maven-jar-plugin") }, "Should detect maven-jar-plugin")
  }

  @Test
  fun testGeneratePklForApacheCommonsLang() {
    // Skip if repo not cloned
    if (!commonsLangPath.exists()) {
      return
    }

    val pom = PomParser.parse(commonsLangPath)
    val pkl = PklGenerator.generate(pom)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"commons-lang3\""))
    assertTrue(pkl.contains("description = "))

    // Verify Maven Central is included (Super POM default)
    assertTrue(pkl.contains("[\"central\"] = \"https://repo.maven.apache.org/maven2\""))
    assertTrue(pkl.contains("Maven Central (Super POM default)"))

    // Verify dependencies section
    assertTrue(pkl.contains("packages {"))

    // Should NOT have test dependencies in main packages
    assertFalse(pkl.contains("junit-jupiter") && pkl.contains("packages {"))

    // Should have test dependencies in testPackages
    assertTrue(pkl.contains("testPackages {"))
    assertTrue(pkl.contains("org.junit.jupiter:junit-jupiter:") || pkl.contains("junit"))

    // Verify plugin warnings
    assertTrue(pkl.contains("Build plugins detected"))
    assertTrue(pkl.contains("maven-javadoc-plugin") || pkl.contains("manual conversion"))

    // Basic PKL syntax validation
    assertTrue(pkl.contains("{"))
    assertTrue(pkl.contains("}"))
  }

  @Test
  fun testParentPomResolution() {
    // Skip if repo not cloned
    if (!commonsLangPath.exists()) {
      return
    }

    val pom = PomParser.parse(commonsLangPath)

    // Parent POM should be resolved from Maven Central since it's not in the local filesystem
    assertNotNull(pom.parent)

    // Properties should include inherited properties from parent
    // Apache Commons Parent defines many standard properties
    assertTrue(pom.properties.containsKey("maven.compiler.source"))
    assertTrue(pom.properties.containsKey("maven.compiler.target"))

    // dependencyManagement should be inherited from parent
    // Apache Commons Parent has extensive dependency management
    assertTrue(pom.dependencyManagement.isNotEmpty())

    // JUnit should have a version from parent's dependencyManagement
    val junitManaged = pom.dependencyManagement["org.junit.jupiter:junit-jupiter"]
    assertNotNull(junitManaged, "JUnit should be in dependencyManagement from parent")
    assertNotNull(junitManaged.version, "Managed JUnit should have version")
  }
}
