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

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Bazel project descriptor containing all relevant build information.
 *
 * @property name Project name
 * @property dependencies List of Maven dependencies from maven_install
 * @property targets List of Bazel targets found
 * @property workspaceFile Path to WORKSPACE or MODULE.bazel file
 * @property buildFile Path to BUILD or BUILD.bazel file
 */
internal data class BazelDescriptor(
  val name: String,
  val dependencies: List<Dependency> = emptyList(),
  val targets: List<Target> = emptyList(),
  val workspaceFile: Path? = null,
  val buildFile: Path? = null,
) {
  data class Dependency(
    val coordinate: String,  // e.g., "com.google.guava:guava:32.1.3-jre"
  ) {
    fun groupId(): String = coordinate.split(":").getOrNull(0) ?: ""
    fun artifactId(): String = coordinate.split(":").getOrNull(1) ?: ""
    fun version(): String? = coordinate.split(":").getOrNull(2)
  }

  data class Target(
    val name: String,
    val rule: String,  // e.g., "java_library", "java_binary", "java_test"
    val srcs: List<String> = emptyList(),
    val deps: List<String> = emptyList(),
  ) {
    fun isTestTarget(): Boolean = rule.contains("test", ignoreCase = true)
  }
}

/**
 * Parser for Bazel build files.
 *
 * Extracts project information from Bazel build files by:
 * 1. Parsing WORKSPACE or MODULE.bazel for maven_install dependencies
 * 2. Parsing BUILD or BUILD.bazel for targets and local dependencies
 *
 * Note: This is a text-based parser that handles common Bazel patterns. For complex
 * builds with dynamic Starlark code, this parser provides best-effort extraction.
 */
internal object BazelParser {
  /**
   * Parse a Bazel project from its workspace and build files.
   *
   * @param projectDir Project directory containing WORKSPACE/MODULE.bazel and BUILD files
   * @return Parsed Bazel project descriptor
   */
  fun parse(projectDir: Path): BazelDescriptor {
    // Find workspace file (WORKSPACE, WORKSPACE.bazel, or MODULE.bazel)
    val workspaceFile = findWorkspaceFile(projectDir)
      ?: throw IllegalArgumentException("No WORKSPACE or MODULE.bazel file found in: $projectDir")

    // Find BUILD file (BUILD or BUILD.bazel)
    val buildFile = findBuildFile(projectDir)

    // Determine project name from directory or workspace
    val projectName = extractProjectName(workspaceFile, projectDir)

    // Parse dependencies from workspace file
    val dependencies = if (workspaceFile.exists()) {
      parseDependencies(workspaceFile)
    } else {
      emptyList()
    }

    // Parse targets from BUILD file
    val targets = if (buildFile?.exists() == true) {
      parseTargets(buildFile)
    } else {
      emptyList()
    }

    return BazelDescriptor(
      name = projectName,
      dependencies = dependencies,
      targets = targets,
      workspaceFile = workspaceFile,
      buildFile = buildFile,
    )
  }

  /**
   * Find WORKSPACE, WORKSPACE.bazel, or MODULE.bazel file.
   */
  private fun findWorkspaceFile(projectDir: Path): Path? {
    val candidates = listOf(
      "MODULE.bazel",
      "WORKSPACE.bazel",
      "WORKSPACE"
    )

    return candidates
      .map { projectDir.resolve(it) }
      .firstOrNull { it.exists() }
  }

  /**
   * Find BUILD or BUILD.bazel file.
   */
  private fun findBuildFile(projectDir: Path): Path? {
    val candidates = listOf(
      "BUILD.bazel",
      "BUILD"
    )

    return candidates
      .map { projectDir.resolve(it) }
      .firstOrNull { it.exists() }
  }

  /**
   * Extract project name from workspace file or directory.
   */
  private fun extractProjectName(workspaceFile: Path, projectDir: Path): String {
    val content = workspaceFile.readText()

    // Try to extract from workspace(name = "...")
    val workspaceNamePattern = """workspace\s*\(\s*name\s*=\s*"([^"]+)"""".toRegex()
    workspaceNamePattern.find(content)?.let {
      return it.groupValues[1]
    }

    // Try to extract from module(name = "...")
    val moduleNamePattern = """module\s*\(\s*name\s*=\s*"([^"]+)"""".toRegex()
    moduleNamePattern.find(content)?.let {
      return it.groupValues[1]
    }

    // Fall back to directory name
    return projectDir.fileName.toString()
  }

  /**
   * Parse Maven dependencies from WORKSPACE/MODULE.bazel file.
   *
   * Looks for maven_install artifacts or maven.artifact declarations.
   */
  private fun parseDependencies(workspaceFile: Path): List<BazelDescriptor.Dependency> {
    val content = workspaceFile.readText()
    val dependencies = mutableListOf<BazelDescriptor.Dependency>()

    // Parse maven_install artifacts = [...]
    val mavenInstallPattern = """maven_install\s*\([^)]*artifacts\s*=\s*\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    mavenInstallPattern.find(content)?.let { match ->
      val artifactsBlock = match.groupValues[1]

      // Extract quoted artifact coordinates
      val artifactPattern = """"([^"]+)"""".toRegex()
      artifactPattern.findAll(artifactsBlock).forEach { artifactMatch ->
        val coordinate = artifactMatch.groupValues[1]
        dependencies.add(BazelDescriptor.Dependency(coordinate))
      }
    }

    // Parse maven.artifact("group:artifact:version")
    val mavenArtifactPattern = """maven\.artifact\s*\(\s*"([^"]+)"""".toRegex()
    mavenArtifactPattern.findAll(content).forEach { match ->
      val coordinate = match.groupValues[1]
      dependencies.add(BazelDescriptor.Dependency(coordinate))
    }

    // Parse maven.install(artifacts = [...])
    val mavenInstallModernPattern = """maven\.install\s*\([^)]*artifacts\s*=\s*\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    mavenInstallModernPattern.find(content)?.let { match ->
      val artifactsBlock = match.groupValues[1]

      // Extract quoted artifact coordinates
      val artifactPattern = """"([^"]+)"""".toRegex()
      artifactPattern.findAll(artifactsBlock).forEach { artifactMatch ->
        val coordinate = artifactMatch.groupValues[1]
        if (!dependencies.any { it.coordinate == coordinate }) {
          dependencies.add(BazelDescriptor.Dependency(coordinate))
        }
      }
    }

    return dependencies
  }

  /**
   * Parse Bazel targets from BUILD file.
   */
  private fun parseTargets(buildFile: Path): List<BazelDescriptor.Target> {
    val content = buildFile.readText()
    val targets = mutableListOf<BazelDescriptor.Target>()

    // Common Java rules
    val javaRules = listOf(
      "java_library",
      "java_binary",
      "java_test",
      "kt_jvm_library",
      "kt_jvm_binary",
      "kt_jvm_test"
    )

    for (rule in javaRules) {
      // Match rule declarations
      val rulePattern = """$rule\s*\(\s*name\s*=\s*"([^"]+)"([^)]*)\)""".toRegex(RegexOption.DOT_MATCHES_ALL)

      rulePattern.findAll(content).forEach { match ->
        val name = match.groupValues[1]
        val body = match.groupValues[2]

        // Extract srcs
        val srcs = extractListAttribute(body, "srcs")

        // Extract deps
        val deps = extractListAttribute(body, "deps")

        targets.add(
          BazelDescriptor.Target(
            name = name,
            rule = rule,
            srcs = srcs,
            deps = deps,
          )
        )
      }
    }

    return targets
  }

  /**
   * Extract a list attribute from a Bazel rule body.
   */
  private fun extractListAttribute(body: String, attributeName: String): List<String> {
    val pattern = """$attributeName\s*=\s*\[(.*?)\]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val match = pattern.find(body) ?: return emptyList()

    val listContent = match.groupValues[1]
    val itemPattern = """"([^"]+)"""".toRegex()

    return itemPattern.findAll(listContent)
      .map { it.groupValues[1] }
      .toList()
  }
}
