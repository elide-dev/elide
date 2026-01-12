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

package elide.tooling.project.adopt.python

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Parser for Python requirements.txt files.
 *
 * Supports parsing:
 * - Package names with version specifiers (e.g., "fastapi>=0.104.0")
 * - Extras (e.g., "uvicorn[standard]>=0.24.0")
 * - Comments (lines starting with #)
 * - Blank lines
 * - -r includes (references to other requirements files)
 * - Environment markers (e.g., "package>=1.0; python_version>='3.8'")
 */
public object RequirementsTxtParser {
  /**
   * Parse a requirements.txt file into a PythonDescriptor.
   *
   * @param path Path to the requirements.txt file
   * @param projectName Optional project name (defaults to directory name)
   * @return Parsed PythonDescriptor
   */
  public fun parse(path: Path, projectName: String? = null): PythonDescriptor {
    val lines = path.readLines()
    val dependencies = mutableListOf<String>()
    val devDependencies = mutableListOf<String>()
    val includedFiles = mutableListOf<Path>()

    for (line in lines) {
      val trimmed = line.trim()

      // Skip blank lines
      if (trimmed.isEmpty()) continue

      // Skip full-line comments
      if (trimmed.startsWith("#")) continue

      // Handle -r includes
      if (trimmed.startsWith("-r ") || trimmed.startsWith("--requirement ")) {
        val includePath = trimmed.removePrefix("-r ").removePrefix("--requirement ").trim()
        val includeFile = path.parent?.resolve(includePath) ?: Path.of(includePath)
        if (includeFile.exists()) {
          includedFiles.add(includeFile)
        }
        continue
      }

      // Parse dependency line
      val (dep, isDev) = parseDependencyLine(trimmed)
      if (dep != null) {
        if (isDev) {
          devDependencies.add(dep)
        } else {
          dependencies.add(dep)
        }
      }
    }

    // Recursively parse included files
    for (includeFile in includedFiles) {
      val included = parse(includeFile, projectName)
      dependencies.addAll(included.dependencies)
      devDependencies.addAll(included.devDependencies)
    }

    val name = projectName ?: path.parent?.fileName?.toString() ?: "python-project"

    return PythonDescriptor(
      name = name,
      dependencies = dependencies.distinct(),
      devDependencies = devDependencies.distinct(),
      configPath = path,
      sourceType = PythonDescriptor.SourceType.REQUIREMENTS_TXT,
    )
  }

  /**
   * Parse a single dependency line from requirements.txt.
   *
   * @param line The line to parse
   * @return Pair of (dependency string, is dev dependency)
   */
  private fun parseDependencyLine(line: String): Pair<String?, Boolean> {
    var content = line

    // Check for inline comments to detect dev dependencies
    val isDev = if (content.contains("#")) {
      val parts = content.split("#", limit = 2)
      content = parts[0].trim()
      val comment = parts[1].trim().lowercase()
      comment.contains("dev") || comment.contains("test")
    } else {
      false
    }

    // Remove environment markers (e.g., "; python_version>='3.8'")
    if (content.contains(";")) {
      content = content.substringBefore(";").trim()
    }

    // Skip options and flags
    if (content.startsWith("-")) return null to false

    // Skip URLs and git repositories
    if (content.contains("://") || content.startsWith("git+")) return null to false

    // Skip editable installs
    if (content.startsWith("-e ") || content.startsWith("--editable ")) return null to false

    // Skip empty content
    if (content.isEmpty()) return null to false

    return content to isDev
  }

  /**
   * Detect if a path is a requirements.txt file.
   *
   * @param path Path to check
   * @return true if the path looks like a requirements file
   */
  public fun isRequirementsFile(path: Path): Boolean {
    val fileName = path.fileName?.toString() ?: return false
    return fileName.matches(Regex("requirements.*\\.txt", RegexOption.IGNORE_CASE))
  }

  /**
   * Find common requirements files in a directory.
   *
   * @param directory Directory to search
   * @return List of found requirements files
   */
  public fun findRequirementsFiles(directory: Path): List<Path> {
    val commonNames = listOf(
      "requirements.txt",
      "requirements-dev.txt",
      "requirements-test.txt",
      "dev-requirements.txt",
      "test-requirements.txt",
    )

    return commonNames
      .map { directory.resolve(it) }
      .filter { it.exists() }
  }

  /**
   * Merge multiple requirements files into a single PythonDescriptor.
   *
   * Typically used to combine requirements.txt, requirements-dev.txt, etc.
   *
   * @param prodFile Path to production requirements.txt
   * @param devFile Optional path to dev requirements file
   * @param projectName Optional project name
   * @return Merged PythonDescriptor
   */
  public fun parseMultiple(
    prodFile: Path,
    devFile: Path? = null,
    projectName: String? = null,
  ): PythonDescriptor {
    val prod = parse(prodFile, projectName)

    return if (devFile != null && devFile.exists()) {
      val dev = parse(devFile, projectName)
      prod.copy(
        devDependencies = (prod.devDependencies + dev.dependencies + dev.devDependencies).distinct(),
      )
    } else {
      prod
    }
  }

  /**
   * Detect and parse the best requirements file strategy for a directory.
   *
   * Priority:
   * 1. requirements.txt + requirements-dev.txt (or dev-requirements.txt)
   * 2. requirements.txt alone
   *
   * @param directory Directory to search
   * @param projectName Optional project name
   * @return Parsed PythonDescriptor or null if no requirements files found
   */
  public fun detectAndParse(directory: Path, projectName: String? = null): PythonDescriptor? {
    val reqFile = directory.resolve("requirements.txt")
    if (!reqFile.exists()) return null

    // Look for dev requirements
    val devFile = listOf(
      "requirements-dev.txt",
      "dev-requirements.txt",
      "requirements-test.txt",
      "test-requirements.txt",
    ).map { directory.resolve(it) }
      .firstOrNull { it.exists() }

    return parseMultiple(reqFile, devFile, projectName)
  }
}
