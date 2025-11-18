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

/**
 * Generates elide.pkl content from a Maven POM descriptor.
 */
object PklGenerator {
  /**
   * Generate elide.pkl content for a multi-module project.
   *
   * @param parentPom The parent/aggregator POM
   * @param modulePoms List of child module POMs
   */
  fun generateMultiModule(parentPom: PomDescriptor, modulePoms: List<PomDescriptor>): String = buildString {
    // Header
    appendLine("amends \"elide:project.pkl\"")
    appendLine()

    // Project metadata from parent
    appendLine("name = \"${parentPom.artifactId}\"")
    if (parentPom.description != null) {
      appendLine("description = \"${parentPom.description.escapeQuotes()}\"")
    }
    if (parentPom.version.isNotBlank()) {
      appendLine("version = \"${parentPom.version}\"")
    }
    appendLine()

    // Workspaces block
    if (parentPom.modules.isNotEmpty()) {
      appendLine("workspaces {")
      parentPom.modules.forEach { module ->
        appendLine("  \"$module\"")
      }
      appendLine("}")
      appendLine()
    }

    // Aggregate all dependencies from all modules
    val allCompileDeps = mutableSetOf<Dependency>()
    val allTestDeps = mutableSetOf<Dependency>()
    val allRepositories = mutableSetOf<Repository>()

    // Include parent dependencies and repositories
    allCompileDeps.addAll(parentPom.dependencies.filter { it.scope == "compile" || it.scope == "runtime" })
    allTestDeps.addAll(parentPom.dependencies.filter { it.scope == "test" })
    allRepositories.addAll(parentPom.repositories)

    // Include module dependencies and repositories
    modulePoms.forEach { modulePom ->
      allCompileDeps.addAll(modulePom.dependencies.filter { it.scope == "compile" || it.scope == "runtime" })
      allTestDeps.addAll(modulePom.dependencies.filter { it.scope == "test" })
      allRepositories.addAll(modulePom.repositories)
    }

    // Filter out inter-module dependencies
    val moduleCoordinates = modulePoms.map { "${it.groupId}:${it.artifactId}" }.toSet()
    val compileDeps = allCompileDeps.filterNot { dep ->
      "${dep.groupId}:${dep.artifactId}" in moduleCoordinates
    }
    val testDeps = allTestDeps.filterNot { dep ->
      "${dep.groupId}:${dep.artifactId}" in moduleCoordinates
    }
    val repositories = allRepositories.toList()

    // Dependencies section
    if (compileDeps.isNotEmpty() || testDeps.isNotEmpty() || repositories.isNotEmpty()) {
      appendLine("dependencies {")
      appendLine("  maven {")

      // Repositories - always include Maven Central (Super POM default)
      val hasMavenCentral = repositories.any {
        it.url.contains("repo.maven.apache.org") || it.url.contains("repo1.maven.org")
      }

      appendLine("    repositories {")

      // Add Maven Central if not already present (Super POM implicit default)
      if (!hasMavenCentral) {
        appendLine("      [\"central\"] = \"https://repo.maven.apache.org/maven2\"  // Maven Central (Super POM default)")
      }

      // Add custom repositories
      repositories.forEach { repo ->
        if (repo.name != null) {
          appendLine("      [\"${repo.id}\"] = \"${repo.url}\"  // ${repo.name}")
        } else {
          appendLine("      [\"${repo.id}\"] = \"${repo.url}\"")
        }
      }
      appendLine("    }")

      // Compile dependencies
      if (compileDeps.isNotEmpty()) {
        appendLine("    packages {")
        compileDeps.sortedBy { it.coordinate() }.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      // Test dependencies
      if (testDeps.isNotEmpty()) {
        appendLine("    testPackages {")
        testDeps.sortedBy { it.coordinate() }.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      appendLine("  }")
      appendLine("}")
      appendLine()
    }

    // Note about multi-module structure
    appendLine("// Note: This is a multi-module Maven project with ${modulePoms.size} module(s).")
    appendLine("// Dependencies are aggregated from all modules. Inter-module dependencies are excluded.")
  }

  /**
   * Generate elide.pkl content from a POM descriptor.
   */
  fun generate(pom: PomDescriptor): String = buildString {
    // Header
    appendLine("amends \"elide:project.pkl\"")
    appendLine()

    // Project metadata
    appendLine("name = \"${pom.artifactId}\"")
    if (pom.description != null) {
      appendLine("description = \"${pom.description.escapeQuotes()}\"")
    }
    appendLine()

    // Dependencies section
    val compileDeps = pom.dependencies.filter { it.scope == "compile" || it.scope == "runtime" }
    val testDeps = pom.dependencies.filter { it.scope == "test" }

    if (compileDeps.isNotEmpty() || testDeps.isNotEmpty() || pom.repositories.isNotEmpty()) {
      appendLine("dependencies {")
      appendLine("  maven {")

      // Repositories - always include Maven Central (Super POM default)
      val hasMavenCentral = pom.repositories.any {
        it.url.contains("repo.maven.apache.org") || it.url.contains("repo1.maven.org")
      }

      appendLine("    repositories {")

      // Add Maven Central if not already present (Super POM implicit default)
      if (!hasMavenCentral) {
        appendLine("      [\"central\"] = \"https://repo.maven.apache.org/maven2\"  // Maven Central (Super POM default)")
      }

      // Add custom repositories
      pom.repositories.forEach { repo ->
        if (repo.name != null) {
          appendLine("      [\"${repo.id}\"] = \"${repo.url}\"  // ${repo.name}")
        } else {
          appendLine("      [\"${repo.id}\"] = \"${repo.url}\"")
        }
      }
      appendLine("    }")

      // Compile dependencies
      if (compileDeps.isNotEmpty()) {
        appendLine("    packages {")
        compileDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      // Test dependencies
      if (testDeps.isNotEmpty()) {
        appendLine("    testPackages {")
        testDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      appendLine("  }")
      appendLine("}")
      appendLine()
    }

    // Source mappings
    appendLine("sources {")
    appendLine("  [\"main\"] = \"src/main/java/**/*.java\"")
    appendLine("  [\"test\"] = \"src/test/java/**/*.java\"")
    appendLine("}")
  }

  /**
   * Generate elide.pkl content from a Gradle project descriptor.
   *
   * @param gradle The Gradle project descriptor
   */
  internal fun generate(gradle: GradleDescriptor): String = buildString {
    // Header
    appendLine("amends \"elide:project.pkl\"")
    appendLine()

    // Project metadata
    appendLine("name = \"${gradle.name}\"")
    if (gradle.description != null) {
      appendLine("description = \"${gradle.description.escapeQuotes()}\"")
    }
    if (gradle.version != "unspecified" && gradle.version.isNotBlank()) {
      appendLine("version = \"${gradle.version}\"")
    }
    appendLine()

    // Dependencies section
    val compileDeps = gradle.dependencies.filterNot { it.isTestScope() }
    val testDeps = gradle.dependencies.filter { it.isTestScope() }

    if (compileDeps.isNotEmpty() || testDeps.isNotEmpty() || gradle.repositories.isNotEmpty()) {
      appendLine("dependencies {")
      appendLine("  maven {")

      // Repositories
      if (gradle.repositories.isNotEmpty()) {
        appendLine("    repositories {")
        gradle.repositories.forEach { repo ->
          appendLine("      [\"${repo.name}\"] = \"${repo.url}\"")
        }
        appendLine("    }")
        appendLine()
      }

      // Compile dependencies
      if (compileDeps.isNotEmpty()) {
        appendLine("    packages {")
        compileDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      // Test dependencies
      if (testDeps.isNotEmpty()) {
        if (compileDeps.isNotEmpty()) appendLine()
        appendLine("    testPackages {")
        testDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      appendLine("  }")
      appendLine("}")
      appendLine()
    }

    // Build warnings
    if (gradle.plugins.isNotEmpty()) {
      appendLine("// Build plugins detected (manual conversion may be needed):")
      gradle.plugins.forEach { plugin ->
        val pluginStr = if (plugin.version != null) {
          "//   - ${plugin.id}:${plugin.version}"
        } else {
          "//   - ${plugin.id}"
        }
        appendLine(pluginStr)
      }
      appendLine()
    }

    // Source mappings (Gradle defaults)
    appendLine("sources {")
    appendLine("  [\"main\"] = \"src/main/java/**/*.java\"")
    appendLine("  [\"test\"] = \"src/test/java/**/*.java\"")
    appendLine("}")
  }

  /**
   * Generate elide.pkl content for a multi-module Gradle project.
   *
   * @param rootProject The root Gradle project
   * @param subprojects List of subproject descriptors
   */
  internal fun generateMultiModule(rootProject: GradleDescriptor, subprojects: List<GradleDescriptor>): String = buildString {
    // Header
    appendLine("amends \"elide:project.pkl\"")
    appendLine()

    // Project metadata from root
    appendLine("name = \"${rootProject.name}\"")
    if (rootProject.description != null) {
      appendLine("description = \"${rootProject.description.escapeQuotes()}\"")
    }
    if (rootProject.version != "unspecified" && rootProject.version.isNotBlank()) {
      appendLine("version = \"${rootProject.version}\"")
    }
    appendLine()

    // Workspaces block
    if (rootProject.modules.isNotEmpty()) {
      appendLine("workspaces {")
      rootProject.modules.forEach { module ->
        appendLine("  \"$module\"")
      }
      appendLine("}")
      appendLine()
    }

    // Aggregate all dependencies from all subprojects
    val allCompileDeps = mutableSetOf<GradleDescriptor.Dependency>()
    val allTestDeps = mutableSetOf<GradleDescriptor.Dependency>()
    val allRepositories = mutableSetOf<GradleDescriptor.Repository>()

    // Include root dependencies and repositories
    allCompileDeps.addAll(rootProject.dependencies.filterNot { it.isTestScope() })
    allTestDeps.addAll(rootProject.dependencies.filter { it.isTestScope() })
    allRepositories.addAll(rootProject.repositories)

    // Include subproject dependencies and repositories
    subprojects.forEach { subproject ->
      allCompileDeps.addAll(subproject.dependencies.filterNot { it.isTestScope() })
      allTestDeps.addAll(subproject.dependencies.filter { it.isTestScope() })
      allRepositories.addAll(subproject.repositories)
    }

    // Dependencies section
    if (allCompileDeps.isNotEmpty() || allTestDeps.isNotEmpty() || allRepositories.isNotEmpty()) {
      appendLine("dependencies {")
      appendLine("  maven {")

      // Repositories
      if (allRepositories.isNotEmpty()) {
        appendLine("    repositories {")
        allRepositories.forEach { repo ->
          appendLine("      [\"${repo.name}\"] = \"${repo.url}\"")
        }
        appendLine("    }")
        appendLine()
      }

      // Compile dependencies
      if (allCompileDeps.isNotEmpty()) {
        appendLine("    packages {")
        allCompileDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      // Test dependencies
      if (allTestDeps.isNotEmpty()) {
        if (allCompileDeps.isNotEmpty()) appendLine()
        appendLine("    testPackages {")
        allTestDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate()}\"")
        }
        appendLine("    }")
      }

      appendLine("  }")
      appendLine("}")
      appendLine()
    }

    // Build warnings (aggregate all plugins)
    val allPlugins = (rootProject.plugins + subprojects.flatMap { it.plugins }).distinctBy { it.id }
    if (allPlugins.isNotEmpty()) {
      appendLine("// Build plugins detected (manual conversion may be needed):")
      allPlugins.forEach { plugin ->
        val pluginStr = if (plugin.version != null) {
          "//   - ${plugin.id}:${plugin.version}"
        } else {
          "//   - ${plugin.id}"
        }
        appendLine(pluginStr)
      }
      appendLine()
    }

    // Source mappings (Gradle defaults)
    appendLine("sources {")
    appendLine("  [\"main\"] = \"src/main/java/**/*.java\"")
    appendLine("  [\"test\"] = \"src/test/java/**/*.java\"")
    appendLine("}")
  }

  private fun String.escapeQuotes(): String = replace("\"", "\\\"")
}
