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
internal data class GradleDescriptor(
  val name: String,
  val group: String = "",
  val version: String = "unspecified",
  val description: String? = null,
  val modules: List<String> = emptyList(),
  val dependencies: List<Dependency> = emptyList(),
  val repositories: List<Repository> = emptyList(),
  val plugins: List<Plugin> = emptyList(),
  val buildFile: Path? = null,
) {
  data class Dependency(
    val configuration: String,  // e.g., implementation, testImplementation
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
  ) {
    fun coordinate(): String = "$groupId:$artifactId${version?.let { ":$it" } ?: ""}"

    fun isTestScope(): Boolean = configuration.contains("test", ignoreCase = true)
  }

  data class Repository(
    val name: String,
    val url: String,
  )

  data class Plugin(
    val id: String,
    val version: String? = null,
  )
}

/**
 * Parser for Gradle build files.
 *
 * Extracts project information from Gradle build files by:
 * 1. Parsing settings.gradle[.kts] for project name and modules
 * 2. Parsing build.gradle[.kts] for metadata, dependencies, and repositories
 * 3. Running `gradle dependencies --configuration compileClasspath` to extract dependency tree
 *
 * Note: This is a text-based parser that handles common Gradle patterns. For complex
 * builds with dynamic configuration, consider using Gradle Tooling API.
 */
internal object GradleParser {
  /**
   * Parse a Gradle project from its build file.
   *
   * @param buildFilePath Path to build.gradle or build.gradle.kts
   * @return Parsed Gradle project descriptor
   */
  fun parse(buildFilePath: Path): GradleDescriptor {
    if (!buildFilePath.exists()) {
      throw IllegalArgumentException("Build file not found: $buildFilePath")
    }

    val projectDir = buildFilePath.parent
    val buildFileContent = buildFilePath.readText()
    val isKotlinDsl = buildFilePath.fileName.toString().endsWith(".kts")

    // Parse settings.gradle[.kts] for project name and modules
    val settingsFile = findSettingsFile(projectDir)
    val (projectName, modules) = if (settingsFile != null) {
      parseSettingsFile(settingsFile)
    } else {
      projectDir.fileName.toString() to emptyList()
    }

    // Parse version catalog if present
    val versionCatalog = GradleVersionCatalogParser.findVersionCatalog(projectDir)?.let {
      GradleVersionCatalogParser.parse(it)
    }

    // Parse build file for metadata
    val group = extractGroup(buildFileContent, isKotlinDsl)
    val version = extractVersion(buildFileContent, isKotlinDsl)
    val description = extractDescription(buildFileContent, isKotlinDsl)

    // Parse dependencies (with version catalog support)
    val dependencies = extractDependencies(buildFileContent, isKotlinDsl, versionCatalog)

    // Parse repositories
    val repositories = extractRepositories(buildFileContent, isKotlinDsl)

    // Parse plugins
    val plugins = extractPlugins(buildFileContent, isKotlinDsl)

    return GradleDescriptor(
      name = projectName,
      group = group,
      version = version,
      description = description,
      modules = modules,
      dependencies = dependencies,
      repositories = repositories,
      plugins = plugins,
      buildFile = buildFilePath,
    )
  }

  /**
   * Find settings.gradle or settings.gradle.kts in the project directory or parent directories.
   */
  private fun findSettingsFile(projectDir: Path): Path? {
    var currentDir: Path? = projectDir

    while (currentDir != null) {
      val settingsGradleKts = currentDir.resolve("settings.gradle.kts")
      if (settingsGradleKts.exists()) return settingsGradleKts

      val settingsGradle = currentDir.resolve("settings.gradle")
      if (settingsGradle.exists()) return settingsGradle

      currentDir = currentDir.parent
    }

    return null
  }

  /**
   * Parse settings file for root project name and included modules.
   */
  private fun parseSettingsFile(settingsFile: Path): Pair<String, List<String>> {
    val content = settingsFile.readText()
    val isKotlinDsl = settingsFile.fileName.toString().endsWith(".kts")

    // Extract root project name
    val namePattern = if (isKotlinDsl) {
      """rootProject\.name\s*=\s*"([^"]+)"""".toRegex()
    } else {
      """rootProject\.name\s*=\s*['"]([^'"]+)['"]""".toRegex()
    }
    val projectName = namePattern.find(content)?.groupValues?.get(1) ?: "root"

    // Extract included modules
    val modules = mutableListOf<String>()

    // Match include("module") or include 'module'
    val includePattern = if (isKotlinDsl) {
      """include\s*\(\s*"([^"]+)"\s*\)""".toRegex()
    } else {
      """include\s+['"]([^'"]+)['"]""".toRegex()
    }

    includePattern.findAll(content).forEach { match ->
      val module = match.groupValues[1].removePrefix(":")
      modules.add(module)
    }

    return projectName to modules
  }

  /**
   * Extract group ID from build file.
   */
  private fun extractGroup(content: String, isKotlinDsl: Boolean): String {
    val pattern = if (isKotlinDsl) {
      """group\s*=\s*"([^"]+)"""".toRegex()
    } else {
      """group\s*=\s*['"]([^'"]+)['"]""".toRegex()
    }
    return pattern.find(content)?.groupValues?.get(1) ?: ""
  }

  /**
   * Extract version from build file.
   */
  private fun extractVersion(content: String, isKotlinDsl: Boolean): String {
    val pattern = if (isKotlinDsl) {
      """version\s*=\s*"([^"]+)"""".toRegex()
    } else {
      """version\s*=\s*['"]([^'"]+)['"]""".toRegex()
    }
    return pattern.find(content)?.groupValues?.get(1) ?: "unspecified"
  }

  /**
   * Extract description from build file.
   */
  private fun extractDescription(content: String, isKotlinDsl: Boolean): String? {
    val pattern = if (isKotlinDsl) {
      """description\s*=\s*"([^"]+)"""".toRegex()
    } else {
      """description\s*=\s*['"]([^'"]+)['"]""".toRegex()
    }
    return pattern.find(content)?.groupValues?.get(1)
  }

  /**
   * Extract dependencies from build file.
   */
  private fun extractDependencies(
    content: String,
    isKotlinDsl: Boolean,
    versionCatalog: VersionCatalog? = null
  ): List<GradleDescriptor.Dependency> {
    val dependencies = mutableListOf<GradleDescriptor.Dependency>()

    // Common configurations
    val configurations = listOf(
      "implementation", "api", "compileOnly", "runtimeOnly",
      "testImplementation", "testCompileOnly", "testRuntimeOnly"
    )

    for (config in configurations) {
      // Use restricted character class that doesn't match newlines
      val pattern = if (isKotlinDsl) {
        """$config\s*\(\s*"([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)"\s*\)""".toRegex()
      } else {
        """$config\s+['"]([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)['"]""".toRegex()
      }

      pattern.findAll(content).forEach { match ->
        dependencies.add(
          GradleDescriptor.Dependency(
            configuration = config,
            groupId = match.groupValues[1],
            artifactId = match.groupValues[2],
            version = match.groupValues[3],
          )
        )
      }

      // Also match without version (use character class that doesn't include newlines)
      val patternNoVersion = if (isKotlinDsl) {
        """$config\s*\(\s*"([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)"\s*\)""".toRegex()
      } else {
        """$config\s+['"]([a-zA-Z0-9._-]+):([a-zA-Z0-9._-]+)['"]""".toRegex()
      }

      patternNoVersion.findAll(content).forEach { match ->
        dependencies.add(
          GradleDescriptor.Dependency(
            configuration = config,
            groupId = match.groupValues[1],
            artifactId = match.groupValues[2],
            version = null,
          )
        )
      }
    }

    // Extract dependencies from version catalog references (libs.*)
    if (versionCatalog != null) {
      for (config in configurations) {
        // Match patterns like: implementation(libs.kotlin.stdlib)
        val catalogPattern = if (isKotlinDsl) {
          """$config\s*\(\s*libs\.([a-zA-Z0-9._-]+)\s*\)""".toRegex()
        } else {
          """$config\s+libs\.([a-zA-Z0-9._-]+)""".toRegex()
        }

        catalogPattern.findAll(content).forEach { match ->
          val catalogRef = match.groupValues[1].replace(".", "-")

          // Check if it's a bundle reference (libs.bundles.xxx)
          if (catalogRef.startsWith("bundles-")) {
            val bundleName = catalogRef.removePrefix("bundles-")
            versionCatalog.bundles[bundleName]?.let { bundleLibs ->
              // Expand bundle into individual dependencies
              bundleLibs.forEach { libAlias ->
                versionCatalog.libraries[libAlias]?.let { library ->
                  val (groupId, artifactId) = library.module.split(":", limit = 2)
                  val version = library.resolveVersion(versionCatalog.versions)
                  dependencies.add(
                    GradleDescriptor.Dependency(
                      configuration = config,
                      groupId = groupId,
                      artifactId = artifactId,
                      version = version,
                    )
                  )
                }
              }
            }
          } else {
            // Try as regular library reference
            versionCatalog.libraries[catalogRef]?.let { library ->
              val (groupId, artifactId) = library.module.split(":", limit = 2)
              val version = library.resolveVersion(versionCatalog.versions)
              dependencies.add(
                GradleDescriptor.Dependency(
                  configuration = config,
                  groupId = groupId,
                  artifactId = artifactId,
                  version = version,
                )
              )
            }
          }
        }
      }
    }

    return dependencies
  }

  /**
   * Extract repositories from build file.
   */
  private fun extractRepositories(content: String, isKotlinDsl: Boolean): List<GradleDescriptor.Repository> {
    val repositories = mutableListOf<GradleDescriptor.Repository>()

    // Check for mavenCentral()
    if (content.contains("mavenCentral()")) {
      repositories.add(
        GradleDescriptor.Repository(
          name = "central",
          url = "https://repo.maven.apache.org/maven2",
        )
      )
    }

    // Check for google()
    if (content.contains("google()")) {
      repositories.add(
        GradleDescriptor.Repository(
          name = "google",
          url = "https://maven.google.com",
        )
      )
    }

    // Check for mavenLocal()
    if (content.contains("mavenLocal()")) {
      repositories.add(
        GradleDescriptor.Repository(
          name = "local",
          url = "file://${System.getProperty("user.home")}/.m2/repository",
        )
      )
    }

    // Extract custom maven repositories
    val urlPattern = if (isKotlinDsl) {
      """maven\s*\{\s*url\s*=\s*uri\s*\(\s*"([^"]+)"\s*\)\s*}""".toRegex()
    } else {
      """maven\s*\{\s*url\s+['"]([^'"]+)['"]""".toRegex()
    }

    urlPattern.findAll(content).forEachIndexed { index, match ->
      repositories.add(
        GradleDescriptor.Repository(
          name = "maven$index",
          url = match.groupValues[1],
        )
      )
    }

    return repositories
  }

  /**
   * Extract plugins from build file.
   */
  private fun extractPlugins(content: String, isKotlinDsl: Boolean): List<GradleDescriptor.Plugin> {
    val plugins = mutableListOf<GradleDescriptor.Plugin>()

    // Extract from plugins block
    val pluginPattern = if (isKotlinDsl) {
      """id\s*\(\s*"([^"]+)"\s*\)(?:\s+version\s+"([^"]+)")?""".toRegex()
    } else {
      """id\s+['"]([^'"]+)['"](?:\s+version\s+['"]([^'"]+)['"])?""".toRegex()
    }

    pluginPattern.findAll(content).forEach { match ->
      plugins.add(
        GradleDescriptor.Plugin(
          id = match.groupValues[1],
          version = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
        )
      )
    }

    // Extract apply plugin statements
    val applyPattern = if (isKotlinDsl) {
      """apply\s*\(\s*plugin\s*=\s*"([^"]+)"\s*\)""".toRegex()
    } else {
      """apply\s+plugin\s*:\s*['"]([^'"]+)['"]""".toRegex()
    }

    applyPattern.findAll(content).forEach { match ->
      val pluginId = match.groupValues[1]
      if (plugins.none { it.id == pluginId }) {
        plugins.add(GradleDescriptor.Plugin(id = pluginId))
      }
    }

    return plugins
  }
}
