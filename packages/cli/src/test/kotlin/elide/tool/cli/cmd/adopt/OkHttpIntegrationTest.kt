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
import kotlin.io.path.isDirectory

/**
 * Integration test for OkHttp - validates Gradle build file parsing.
 *
 * OkHttp is a popular HTTP client library that uses:
 * - Kotlin DSL (build.gradle.kts)
 * - Multi-module project structure
 * - Maven Central and custom repositories
 * - Multiple Gradle plugins
 * - Dependency version catalogs
 *
 * This test uses the cloned OkHttp repository at /tmp/okhttp or
 * /private/tmp/okhttp.
 * If the repository doesn't exist, the test is skipped.
 */
class OkHttpIntegrationTest {
  private val okHttpPath = findOkHttpPath()

  private fun findOkHttpPath(): Path? {
    val candidates = listOf(
      Paths.get("/tmp/okhttp"),
      Paths.get("/private/tmp/okhttp")
    )
    return candidates.firstOrNull { it.exists() && it.isDirectory() }
  }

  @BeforeTest
  fun checkPreconditions() {
    if (okHttpPath == null) {
      println("Skipping OkHttp integration test - repository not found")
      println("To enable this test, clone:")
      println("  git clone --depth 1 https://github.com/square/okhttp.git /tmp/okhttp")
    }
  }

  @Test
  fun testParseOkHttpRootBuild() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    // Find root build file (should be build.gradle.kts)
    val buildFileKts = okHttpPath!!.resolve("build.gradle.kts")
    val buildFileGroovy = okHttpPath!!.resolve("build.gradle")

    val buildFile = when {
      buildFileKts.exists() -> buildFileKts
      buildFileGroovy.exists() -> buildFileGroovy
      else -> {
        println("Skipping - no build file found at $okHttpPath")
        return
      }
    }

    val project = GradleParser.parse(buildFile)

    // Verify basic project info
    assertNotNull(project.name, "Should have project name")
    assertTrue(project.name.isNotEmpty(), "Project name should not be empty")

    // OkHttp typically has a group ID
    if (project.group.isNotBlank()) {
      assertTrue(
        project.group.startsWith("com.squareup"),
        "OkHttp group should start with com.squareup: ${project.group}"
      )
    }

    println("Parsed OkHttp root project: ${project.name}")
  }

  @Test
  fun testMultiModuleStructure() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return

    val project = GradleParser.parse(buildFile)

    // OkHttp is a multi-module project
    if (project.modules.isNotEmpty()) {
      println("Detected ${project.modules.size} modules:")
      project.modules.forEach { println("  - $it") }

      // Verify at least some modules exist
      val moduleCount = project.modules.count { moduleName ->
        val modulePath = okHttpPath!!.resolve(moduleName)
        modulePath.exists() && modulePath.isDirectory()
      }

      assertTrue(
        moduleCount > 0,
        "At least some declared modules should exist as directories"
      )
    }
  }

  @Test
  fun testParseModuleBuild() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val rootBuildFile = findBuildFile(okHttpPath!!) ?: return
    val rootProject = GradleParser.parse(rootBuildFile)

    if (rootProject.modules.isEmpty()) {
      println("Not a multi-module project, skipping module test")
      return
    }

    // Try to parse the first module
    val moduleName = rootProject.modules.firstOrNull() ?: return

    val moduleBuildFile = findBuildFile(okHttpPath!!.resolve(moduleName))
    if (moduleBuildFile != null) {
      val moduleProject = GradleParser.parse(moduleBuildFile)

      println("Successfully parsed module: ${moduleProject.name}")

      // Module should have dependencies
      if (moduleProject.dependencies.isNotEmpty()) {
        println("  Dependencies: ${moduleProject.dependencies.size}")

        // Verify dependency structure
        moduleProject.dependencies.forEach { dep ->
          assertTrue(dep.groupId.isNotEmpty(), "Dependency should have groupId")
          assertTrue(dep.artifactId.isNotEmpty(), "Dependency should have artifactId")
          assertTrue(dep.configuration.isNotEmpty(), "Dependency should have configuration")
        }
      }

      // Module should have plugins
      if (moduleProject.plugins.isNotEmpty()) {
        println("  Plugins: ${moduleProject.plugins.size}")
        moduleProject.plugins.forEach { plugin ->
          assertTrue(plugin.id.isNotEmpty(), "Plugin should have ID")
        }
      }
    }
  }

  @Test
  fun testRepositoryDetection() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val project = GradleParser.parse(buildFile)

    // OkHttp typically uses Maven Central
    val hasMavenCentral = project.repositories.any {
      it.name == "central" || it.url.contains("maven.apache.org")
    }

    if (hasMavenCentral) {
      println("Found Maven Central repository")
    }

    // Verify repository structure
    project.repositories.forEach { repo ->
      assertTrue(repo.name.isNotEmpty(), "Repository should have name")
      assertTrue(repo.url.isNotEmpty(), "Repository should have URL")
      assertTrue(repo.url.startsWith("http") || repo.url.startsWith("file:"), "Repository URL should be valid")
    }
  }

  @Test
  fun testDependencyParsing() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val project = GradleParser.parse(buildFile)

    if (project.dependencies.isEmpty()) {
      println("Root project has no dependencies (common for parent POMs)")
      return
    }

    println("Found ${project.dependencies.size} dependencies")

    // Categorize dependencies
    val implementationDeps = project.dependencies.filter {
      it.configuration.contains("implementation", ignoreCase = true) &&
      !it.configuration.contains("test", ignoreCase = true)
    }

    val testDeps = project.dependencies.filter {
      it.configuration.contains("test", ignoreCase = true)
    }

    println("  Implementation: ${implementationDeps.size}")
    println("  Test: ${testDeps.size}")

    // Verify dependency structure
    project.dependencies.forEach { dep ->
      assertTrue(dep.groupId.isNotEmpty(), "Dependency should have groupId")
      assertTrue(dep.artifactId.isNotEmpty(), "Dependency should have artifactId")
      assertFalse(dep.isTestScope() && dep.configuration == "implementation", "Test scope detection should work")
    }
  }

  @Test
  fun testPluginDetection() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val project = GradleParser.parse(buildFile)

    // Gradle projects typically have plugins
    if (project.plugins.isNotEmpty()) {
      println("Found ${project.plugins.size} plugins:")

      project.plugins.forEach { plugin ->
        println("  - ${plugin.id}${plugin.version?.let { ":$it" } ?: ""}")
        assertTrue(plugin.id.isNotEmpty(), "Plugin should have ID")
      }

      // Common plugins in Kotlin/Android projects
      val commonPluginPrefixes = listOf(
        "org.jetbrains.kotlin",
        "com.android",
        "java",
        "application",
        "maven-publish"
      )

      val hasCommonPlugin = project.plugins.any { plugin ->
        commonPluginPrefixes.any { prefix ->
          plugin.id.startsWith(prefix)
        }
      }

      if (hasCommonPlugin) {
        println("  Found common Kotlin/Java/Android plugin")
      }
    }
  }

  @Test
  fun testGeneratePklForGradle() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val project = GradleParser.parse(buildFile)

    val pkl = PklGenerator.generate(project)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""), "Should amend elide:project.pkl")
    assertTrue(pkl.contains("name = "), "Should have project name")

    // Verify dependencies section if project has dependencies
    if (project.dependencies.isNotEmpty()) {
      assertTrue(pkl.contains("dependencies {"), "Should have dependencies section")
      assertTrue(pkl.contains("maven {"), "Should have maven subsection")
    }

    // Verify repositories if present
    if (project.repositories.isNotEmpty()) {
      assertTrue(pkl.contains("repositories {"), "Should have repositories section")
    }

    // Verify plugins comment if present
    if (project.plugins.isNotEmpty()) {
      assertTrue(
        pkl.contains("Build plugins detected"),
        "Should document plugins"
      )
    }

    // Basic PKL syntax validation
    val braceCount = pkl.count { it == '{' }
    val closeBraceCount = pkl.count { it == '}' }
    assertEquals(braceCount, closeBraceCount, "Braces should be balanced")

    println("Generated PKL (${pkl.lines().size} lines)")
  }

  @Test
  fun testGenerateMultiModulePkl() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val rootProject = GradleParser.parse(buildFile)

    if (rootProject.modules.isEmpty()) {
      println("Not a multi-module project, skipping multi-module PKL test")
      return
    }

    // Parse available modules
    val modulePoms = rootProject.modules.mapNotNull { moduleName ->
      val moduleBuildFile = findBuildFile(okHttpPath!!.resolve(moduleName))
      if (moduleBuildFile != null) {
        try {
          GradleParser.parse(moduleBuildFile)
        } catch (e: Exception) {
          println("Warning: Could not parse module $moduleName: ${e.message}")
          null
        }
      } else {
        null
      }
    }

    if (modulePoms.isEmpty()) {
      println("No modules could be parsed")
      return
    }

    val pkl = PklGenerator.generateMultiModule(rootProject, modulePoms)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""), "Should amend elide:project.pkl")

    // Should have workspaces
    if (rootProject.modules.isNotEmpty()) {
      assertTrue(
        pkl.contains("workspaces {"),
        "Multi-module PKL should have workspaces section"
      )
    }

    // Basic syntax validation
    val braceCount = pkl.count { it == '{' }
    val closeBraceCount = pkl.count { it == '}' }
    assertEquals(braceCount, closeBraceCount, "Braces should be balanced")

    println("Generated multi-module PKL (${pkl.lines().size} lines)")
    println("  Included ${modulePoms.size} modules")
  }

  @Test
  fun testKotlinDslDetection() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFileKts = okHttpPath!!.resolve("build.gradle.kts")

    if (buildFileKts.exists()) {
      println("Detected Kotlin DSL (build.gradle.kts)")

      val project = GradleParser.parse(buildFileKts)

      // Should successfully parse Kotlin DSL
      assertNotNull(project.name, "Should parse Kotlin DSL project name")

      // Verify build file reference
      assertEquals(buildFileKts, project.buildFile, "Should reference correct build file")
    } else {
      println("No Kotlin DSL build file found")
    }
  }

  @Test
  fun testDependencyConfigurations() {
    // Skip if repo not cloned
    if (okHttpPath == null) {
      return
    }

    val buildFile = findBuildFile(okHttpPath!!) ?: return
    val project = GradleParser.parse(buildFile)

    if (project.dependencies.isEmpty()) {
      return
    }

    // Common Gradle configurations
    val configurations = project.dependencies.map { it.configuration }.toSet()

    println("Found configurations: ${configurations.joinToString(", ")}")

    // Verify configuration names are reasonable
    configurations.forEach { config ->
      assertTrue(
        config.matches(Regex("[a-zA-Z][a-zA-Z0-9]*")),
        "Configuration name should be valid: $config"
      )
    }

    // Test scope detection
    val testDeps = project.dependencies.filter { it.isTestScope() }
    testDeps.forEach { dep ->
      assertTrue(
        dep.configuration.contains("test", ignoreCase = true),
        "Test dependency should have test in configuration: ${dep.configuration}"
      )
    }
  }

  /**
   * Find build file (try Kotlin DSL first, then Groovy DSL).
   */
  private fun findBuildFile(projectDir: Path): Path? {
    val kts = projectDir.resolve("build.gradle.kts")
    if (kts.exists()) return kts

    val groovy = projectDir.resolve("build.gradle")
    if (groovy.exists()) return groovy

    return null
  }
}
