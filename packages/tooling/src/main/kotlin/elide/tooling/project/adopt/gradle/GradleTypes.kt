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

/**
 * Gradle project descriptor containing all relevant build information.
 *
 * @property name Project name
 * @property group Project group ID
 * @property version Project version
 * @property description Project description
 * @property modules List of subproject modules (for multi-module projects)
 * @property dependencies List of project dependencies
 * @property repositories List of repository URLs
 * @property plugins List of applied plugins
 * @property buildFile Path to the build file (build.gradle or build.gradle.kts)
 */
public data class GradleDescriptor(
  val name: String,
  val group: String = "",
  val version: String = "unspecified",
  val description: String? = null,
  val modules: List<String> = emptyList(),
  val dependencies: List<Dependency> = emptyList(),
  val repositories: List<Repository> = emptyList(),
  val plugins: List<Plugin> = emptyList(),
  val includedBuilds: List<String> = emptyList(),
  val buildFile: Path? = null,
) {
  /**
   * Represents a Gradle dependency.
   */
  public data class Dependency(
    val configuration: String,  // e.g., implementation, testImplementation
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
  ) {
    /** Get Maven coordinate string. */
    public fun coordinate(): String = "$groupId:$artifactId${version?.let { ":$it" } ?: ""}"

    /** Check if this is a test-scoped dependency. */
    public fun isTestScope(): Boolean = configuration.contains("test", ignoreCase = true)

    /** Check if this is a compile-only dependency. */
    public fun isCompileOnly(): Boolean = configuration.contains("compileOnly", ignoreCase = true)
  }

  /**
   * Represents a Gradle repository.
   */
  public data class Repository(
    val name: String,
    val url: String,
  )

  /**
   * Represents a Gradle plugin.
   */
  public data class Plugin(
    val id: String,
    val version: String? = null,
  )
}

/**
 * Data class representing a Gradle version catalog.
 *
 * @property versions Map of version variable names to version strings
 * @property libraries Map of library aliases to library declarations
 * @property bundles Map of bundle names to lists of library aliases
 * @property plugins Map of plugin aliases to plugin declarations
 */
public data class VersionCatalog(
  val versions: Map<String, String> = emptyMap(),
  val libraries: Map<String, Library> = emptyMap(),
  val bundles: Map<String, List<String>> = emptyMap(),
  val plugins: Map<String, Plugin> = emptyMap(),
) {
  /**
   * Represents a library declaration in the version catalog.
   *
   * @property module The Maven coordinates (groupId:artifactId)
   * @property version The version (direct or ref to [versions])
   * @property versionRef Reference to a version in [versions] map
   */
  public data class Library(
    val module: String,
    val version: String? = null,
    val versionRef: String? = null,
  ) {
    /**
     * Resolve the actual version using the version catalog's versions map.
     */
    public fun resolveVersion(versions: Map<String, String>): String? {
      return when {
        version != null -> version
        versionRef != null -> versions[versionRef]
        else -> null
      }
    }
  }

  /**
   * Represents a plugin declaration in the version catalog.
   *
   * @property id The plugin ID
   * @property version The version (direct or ref to [versions])
   * @property versionRef Reference to a version in [versions] map
   */
  public data class Plugin(
    val id: String,
    val version: String? = null,
    val versionRef: String? = null,
  ) {
    /**
     * Resolve the actual version using the version catalog's versions map.
     */
    public fun resolveVersion(versions: Map<String, String>): String? {
      return when {
        version != null -> version
        versionRef != null -> versions[versionRef]
        else -> null
      }
    }
  }
}
