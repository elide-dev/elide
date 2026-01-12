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
import java.nio.file.Paths

/** Tests for PKL generator functionality. */
class PklGeneratorTest {
  @Test
  fun testGenerateBasicPom() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = "Test Project",
      description = "A test project",
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = emptyList(),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    assertTrue(pkl.contains("""amends "elide:project.pkl""""))
    assertTrue(pkl.contains("""name = "test-project""""))
    assertTrue(pkl.contains("""description = "A test project""""))
  }

  @Test
  fun testMavenCentralAutoIncluded() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.google.guava", "guava", "32.1.3-jre", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),  // No explicit repositories
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    // Should include Maven Central by default
    assertTrue(pkl.contains("""["central"] = "https://repo.maven.apache.org/maven2""""))
    assertTrue(pkl.contains("Maven Central (Super POM default)"))
  }

  @Test
  fun testMavenCentralNotDuplicatedWhenExplicit() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.google.guava", "guava", "32.1.3-jre", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = listOf(
        Repository("central", "https://repo.maven.apache.org/maven2", "Maven Central")
      ),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    // Should not duplicate Maven Central
    val centralCount = pkl.split("""["central"]""").size - 1
    assertEquals(1, centralCount, "Maven Central should appear exactly once")
  }

  @Test
  fun testCustomRepositoriesIncluded() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.example", "lib", "1.0.0", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = listOf(
        Repository("jitpack.io", "https://jitpack.io", "JitPack")
      ),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    // Should include both Maven Central and JitPack
    assertTrue(pkl.contains("""["central"] = "https://repo.maven.apache.org/maven2""""))
    assertTrue(pkl.contains("""["jitpack.io"] = "https://jitpack.io""""))
    assertTrue(pkl.contains("JitPack"))
  }

  @Test
  fun testCompileAndTestDependenciesSeparated() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.google.guava", "guava", "32.1.3-jre", "compile"),
        Dependency("junit", "junit", "4.13.2", "test")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    // Check packages section
    assertTrue(pkl.contains("packages {"))
    assertTrue(pkl.contains("""      "com.google.guava:guava:32.1.3-jre""""))

    // Check testPackages section
    assertTrue(pkl.contains("testPackages {"))
    assertTrue(pkl.contains("""      "junit:junit:4.13.2""""))
  }

  @Test
  fun testMultiModuleGeneration() {
    val parentPom = PomDescriptor(
      groupId = "com.example",
      artifactId = "parent-project",
      version = "1.0.0",
      packaging = "pom",
      name = "Parent Project",
      description = "Multi-module parent",
      modules = listOf("module-a", "module-b"),
      properties = emptyMap(),
      dependencies = emptyList(),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val moduleAPom = PomDescriptor(
      groupId = "com.example",
      artifactId = "module-a",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.google.guava", "guava", "32.1.3-jre", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/module-a/pom.xml")
    )

    val moduleBPom = PomDescriptor(
      groupId = "com.example",
      artifactId = "module-b",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("org.slf4j", "slf4j-api", "2.0.9", "compile"),
        // Inter-module dependency (should be filtered out)
        Dependency("com.example", "module-a", "1.0.0", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/module-b/pom.xml")
    )

    val pkl = PklGenerator.generateMultiModule(parentPom, listOf(moduleAPom, moduleBPom))

    // Check workspaces
    assertTrue(pkl.contains("workspaces {"))
    assertTrue(pkl.contains(""""module-a""""))
    assertTrue(pkl.contains(""""module-b""""))

    // Check aggregated dependencies
    assertTrue(pkl.contains("""com.google.guava:guava:32.1.3-jre"""))
    assertTrue(pkl.contains("""org.slf4j:slf4j-api:2.0.9"""))

    // Inter-module dependency should be filtered out
    assertFalse(pkl.contains("""com.example:module-a:1.0.0"""))

    // Check for note about multi-module structure
    assertTrue(pkl.contains("multi-module Maven project"))
  }

  @Test
  fun testMultiModuleMavenCentralIncluded() {
    val parentPom = PomDescriptor(
      groupId = "com.example",
      artifactId = "parent-project",
      version = "1.0.0",
      packaging = "pom",
      name = "Parent Project",
      description = null,
      modules = listOf("module-a"),
      properties = emptyMap(),
      dependencies = emptyList(),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val moduleAPom = PomDescriptor(
      groupId = "com.example",
      artifactId = "module-a",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = null,
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = listOf(
        Dependency("com.google.guava", "guava", "32.1.3-jre", "compile")
      ),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/module-a/pom.xml")
    )

    val pkl = PklGenerator.generateMultiModule(parentPom, listOf(moduleAPom))

    // Should include Maven Central by default
    assertTrue(pkl.contains("""["central"] = "https://repo.maven.apache.org/maven2""""))
    assertTrue(pkl.contains("Maven Central (Super POM default)"))
  }

  @Test
  fun testDescriptionEscapesQuotes() {
    val pom = PomDescriptor(
      groupId = "com.example",
      artifactId = "test-project",
      version = "1.0.0",
      packaging = "jar",
      name = null,
      description = """A project with "quoted" text""",
      modules = emptyList(),
      properties = emptyMap(),
      dependencies = emptyList(),
      dependencyManagement = emptyMap(),
      parent = null,
      repositories = emptyList(),
      profiles = emptyList(),
      plugins = emptyList(),
      path = Paths.get("/tmp/pom.xml")
    )

    val pkl = PklGenerator.generate(pom)

    // Quotes should be escaped
    assertTrue(pkl.contains("""A project with \"quoted\" text"""))
  }
}
