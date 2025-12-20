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

package elide.tooling.project.adopt.gradle

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Parser for Gradle version catalogs (libs.versions.toml).
 *
 * Supports parsing:
 * - [versions] section
 * - [libraries] section
 * - [bundles] section
 * - [plugins] section
 */
public object VersionCatalogParser {
  /**
   * Parse a version catalog file.
   *
   * @param catalogPath Path to the libs.versions.toml file
   * @return Parsed VersionCatalog
   */
  public fun parse(catalogPath: Path): VersionCatalog {
    if (!catalogPath.exists()) {
      return VersionCatalog()
    }

    val lines = catalogPath.readLines()
    var currentSection: String? = null
    val versions = mutableMapOf<String, String>()
    val libraries = mutableMapOf<String, VersionCatalog.Library>()
    val bundles = mutableMapOf<String, List<String>>()
    val plugins = mutableMapOf<String, VersionCatalog.Plugin>()

    for (line in lines) {
      val trimmed = line.trim()

      // Skip empty lines and comments
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

      // Section header
      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
        currentSection = trimmed.substring(1, trimmed.length - 1)
        continue
      }

      // Parse based on current section
      when (currentSection) {
        "versions" -> parseVersionLine(trimmed)?.let { (key, value) ->
          versions[key] = value
        }
        "libraries" -> parseLibraryLine(trimmed)?.let { (key, lib) ->
          libraries[key] = lib
        }
        "bundles" -> parseBundleLine(trimmed)?.let { (key, libs) ->
          bundles[key] = libs
        }
        "plugins" -> parsePluginLine(trimmed)?.let { (key, plugin) ->
          plugins[key] = plugin
        }
      }
    }

    return VersionCatalog(
      versions = versions,
      libraries = libraries,
      bundles = bundles,
      plugins = plugins
    )
  }

  /**
   * Parse a version line: `name = "version"`
   */
  private fun parseVersionLine(line: String): Pair<String, String>? {
    val parts = line.split("=", limit = 2)
    if (parts.size != 2) return null

    val key = parts[0].trim()
    // Strip any trailing comments first, then remove quotes, then trim again
    val value = parts[1].trim()
      .substringBefore("#").trim()
      .removeSurrounding("\"")
      .trim()

    return key to value
  }

  /**
   * Parse a library line.
   * Formats:
   * - `alias = { module = "group:artifact", version.ref = "versionName" }`
   * - `alias = { module = "group:artifact", version = "1.0.0" }`
   * - `alias = { module = "group:artifact" }`
   */
  private fun parseLibraryLine(line: String): Pair<String, VersionCatalog.Library>? {
    val parts = line.split("=", limit = 2)
    if (parts.size != 2) return null

    val alias = parts[0].trim()
    val declaration = parts[1].trim()

    // Parse the inline table: { module = "...", version.ref = "..." }
    val module = extractValue(declaration, "module") ?: return null
    val version = extractValue(declaration, "version")
    val versionRef = extractValue(declaration, "version.ref")

    return alias to VersionCatalog.Library(
      module = module,
      version = version,
      versionRef = versionRef
    )
  }

  /**
   * Parse a bundle line: `bundleName = ["lib1", "lib2", "lib3"]`
   */
  private fun parseBundleLine(line: String): Pair<String, List<String>>? {
    val parts = line.split("=", limit = 2)
    if (parts.size != 2) return null

    val bundleName = parts[0].trim()
    val librariesStr = parts[1].trim()

    // Extract array: ["lib1", "lib2"]
    val libraries = librariesStr
      .removeSurrounding("[", "]")
      .split(",")
      .map { it.trim().removeSurrounding("\"") }
      .filter { it.isNotEmpty() }

    return bundleName to libraries
  }

  /**
   * Parse a plugin line.
   * Format: `alias = { id = "plugin.id", version.ref = "versionName" }`
   */
  private fun parsePluginLine(line: String): Pair<String, VersionCatalog.Plugin>? {
    val parts = line.split("=", limit = 2)
    if (parts.size != 2) return null

    val alias = parts[0].trim()
    val declaration = parts[1].trim()

    // Parse the inline table
    val id = extractValue(declaration, "id") ?: return null
    val version = extractValue(declaration, "version")
    val versionRef = extractValue(declaration, "version.ref")

    return alias to VersionCatalog.Plugin(
      id = id,
      version = version,
      versionRef = versionRef
    )
  }

  /**
   * Extract a value from an inline TOML table.
   * Example: `{ module = "value", version.ref = "ref" }` -> extractValue("module") = "value"
   */
  private fun extractValue(inlineTable: String, key: String): String? {
    // Remove braces
    val content = inlineTable.removeSurrounding("{", "}").trim()

    // Split by comma (simple approach - doesn't handle nested structures)
    val pairs = content.split(",")

    for (pair in pairs) {
      val parts = pair.split("=", limit = 2)
      if (parts.size != 2) continue

      val pairKey = parts[0].trim()
      val pairValue = parts[1].trim().removeSurrounding("\"")

      if (pairKey == key) {
        return pairValue
      }
    }

    return null
  }

  /**
   * Find the version catalog file in a project directory.
   * Looks for: gradle/libs.versions.toml
   */
  public fun findVersionCatalog(projectDir: Path): Path? {
    val catalogPath = projectDir.resolve("gradle/libs.versions.toml")
    return if (catalogPath.exists()) catalogPath else null
  }
}
