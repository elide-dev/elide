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

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Parser for Python pyproject.toml files (PEP 621).
 *
 * Supports parsing:
 * - [project] metadata (name, version, description)
 * - [project.dependencies]
 * - [project.optional-dependencies]
 * - [project.scripts]
 * - [build-system]
 * - requires-python version constraints
 */
public object PyProjectParser {
  /**
   * Parse a pyproject.toml file into a PythonDescriptor.
   *
   * @param path Path to the pyproject.toml file
   * @return Parsed PythonDescriptor
   * @throws IllegalArgumentException if the file is invalid or missing required fields
   */
  public fun parse(path: Path): PythonDescriptor {
    val content = path.readText()
    val toml = Toml(
      inputConfig = TomlInputConfig(
        ignoreUnknownNames = true,
        allowEmptyValues = true,
        allowNullValues = true,
        allowEscapedQuotesInLiteralStrings = true,
      ),
      outputConfig = TomlOutputConfig(
        indentation = TomlIndentation.FOUR_SPACES,
      )
    )

    val pyproject = toml.decodeFromString(PyProjectToml.serializer(), content)
    val project = pyproject.project
      ?: throw IllegalArgumentException("pyproject.toml missing [project] section")

    val name = project.name
      ?: throw IllegalArgumentException("pyproject.toml missing project.name")

    return PythonDescriptor(
      name = name,
      version = project.version,
      description = project.description,
      pythonVersion = project.requiresPython,
      dependencies = project.dependencies ?: emptyList(),
      devDependencies = emptyList(), // Dev deps are in optional-dependencies
      optionalDependencies = project.optionalDependencies ?: emptyMap(),
      scripts = project.scripts ?: emptyMap(),
      buildSystem = pyproject.buildSystem?.buildBackend,
      configPath = path,
      sourceType = PythonDescriptor.SourceType.PYPROJECT_TOML,
    )
  }

  /**
   * Check if a file is a valid pyproject.toml.
   *
   * @param path Path to check
   * @return true if the file exists and contains a [project] section
   */
  public fun isValidPyProjectToml(path: Path): Boolean {
    return try {
      val content = path.readText()
      content.contains("[project]") && content.contains("name")
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Extract dev dependencies from optional-dependencies.
   *
   * Common patterns: "dev", "test", "testing", "development"
   *
   * @param descriptor PythonDescriptor with optional dependencies
   * @return Updated descriptor with dev dependencies extracted
   */
  public fun extractDevDependencies(descriptor: PythonDescriptor): PythonDescriptor {
    val devKeys = setOf("dev", "test", "testing", "development", "dev-dependencies")
    val devDeps = mutableListOf<String>()
    val remainingOptional = descriptor.optionalDependencies.toMutableMap()

    for (key in devKeys) {
      remainingOptional.remove(key)?.let { deps ->
        devDeps.addAll(deps)
      }
    }

    return descriptor.copy(
      devDependencies = devDeps,
      optionalDependencies = remainingOptional,
    )
  }
}

/**
 * TOML representation of pyproject.toml (PEP 621).
 */
@Serializable
internal data class PyProjectToml(
  @SerialName("project")
  val project: Project? = null,

  @SerialName("build-system")
  val buildSystem: BuildSystem? = null,

  @SerialName("tool")
  val tool: Map<String, ToolConfig>? = null,
) {
  /**
   * [project] section (PEP 621).
   */
  @Serializable
  data class Project(
    @SerialName("name")
    val name: String? = null,

    @SerialName("version")
    val version: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("requires-python")
    val requiresPython: String? = null,

    @SerialName("dependencies")
    val dependencies: List<String>? = null,

    @SerialName("optional-dependencies")
    val optionalDependencies: Map<String, List<String>>? = null,

    @SerialName("scripts")
    val scripts: Map<String, String>? = null,

    @SerialName("readme")
    val readme: String? = null,

    @SerialName("license")
    val license: License? = null,

    @SerialName("authors")
    val authors: List<Person>? = null,

    @SerialName("maintainers")
    val maintainers: List<Person>? = null,

    @SerialName("keywords")
    val keywords: List<String>? = null,

    @SerialName("classifiers")
    val classifiers: List<String>? = null,

    @SerialName("urls")
    val urls: Map<String, String>? = null,
  )

  /**
   * [build-system] section (PEP 517/518).
   */
  @Serializable
  data class BuildSystem(
    @SerialName("requires")
    val requires: List<String>? = null,

    @SerialName("build-backend")
    val buildBackend: String? = null,

    @SerialName("backend-path")
    val backendPath: List<String>? = null,
  )

  /**
   * License information.
   */
  @Serializable
  data class License(
    @SerialName("text")
    val text: String? = null,

    @SerialName("file")
    val file: String? = null,
  )

  /**
   * Person (author/maintainer).
   */
  @Serializable
  data class Person(
    @SerialName("name")
    val name: String? = null,

    @SerialName("email")
    val email: String? = null,
  )

  /**
   * Tool-specific configuration (e.g., [tool.poetry], [tool.black]).
   */
  @Serializable
  data class ToolConfig(
    val config: Map<String, String> = emptyMap(),
  )
}
