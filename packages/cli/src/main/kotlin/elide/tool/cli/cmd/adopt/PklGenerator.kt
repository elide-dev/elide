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

      // Repositories
      if (repositories.isNotEmpty()) {
        appendLine("    repositories {")
        repositories.forEach { repo ->
          if (repo.name != null) {
            appendLine("      [\"${repo.id}\"] = \"${repo.url}\"  // ${repo.name}")
          } else {
            appendLine("      [\"${repo.id}\"] = \"${repo.url}\"")
          }
        }
        appendLine("    }")
      }

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

      // Repositories
      if (pom.repositories.isNotEmpty()) {
        appendLine("    repositories {")
        pom.repositories.forEach { repo ->
          if (repo.name != null) {
            appendLine("      [\"${repo.id}\"] = \"${repo.url}\"  // ${repo.name}")
          } else {
            appendLine("      [\"${repo.id}\"] = \"${repo.url}\"")
          }
        }
        appendLine("    }")
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

  private fun String.escapeQuotes(): String = replace("\"", "\\\"")
}
