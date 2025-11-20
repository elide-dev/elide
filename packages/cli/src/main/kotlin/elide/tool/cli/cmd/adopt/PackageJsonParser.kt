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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Node.js project descriptor parsed from package.json.
 *
 * @property name Package name
 * @property version Package version
 * @property description Package description
 * @property dependencies Production dependencies
 * @property devDependencies Development dependencies
 * @property peerDependencies Peer dependencies
 * @property optionalDependencies Optional dependencies
 * @property workspaces Workspaces configuration (for monorepos)
 * @property scripts NPM scripts
 * @property packageJsonPath Path to package.json file
 */
internal data class PackageJsonDescriptor(
  val name: String,
  val version: String? = null,
  val description: String? = null,
  val dependencies: Map<String, String> = emptyMap(),
  val devDependencies: Map<String, String> = emptyMap(),
  val peerDependencies: Map<String, String> = emptyMap(),
  val optionalDependencies: Map<String, String> = emptyMap(),
  val workspaces: List<String> = emptyList(),
  val scripts: Map<String, String> = emptyMap(),
  val packageJsonPath: Path? = null,
)

/**
 * Serializable package.json structure for parsing with kotlinx.serialization.
 */
@Serializable
private data class PackageJson(
  val name: String,
  val version: String? = null,
  val description: String? = null,
  val dependencies: Map<String, String>? = null,
  val devDependencies: Map<String, String>? = null,
  val peerDependencies: Map<String, String>? = null,
  val optionalDependencies: Map<String, String>? = null,
  val workspaces: WorkspacesConfig? = null,
  val scripts: Map<String, String>? = null,
)

/**
 * Workspaces configuration - can be array or object with packages array.
 */
@Serializable(with = WorkspacesConfigSerializer::class)
private sealed class WorkspacesConfig {
  data class Array(val packages: List<String>) : WorkspacesConfig()
  data class Object(val packages: List<String>) : WorkspacesConfig()

  fun packages(): List<String> = when (this) {
    is Array -> packages
    is Object -> packages
  }
}

/**
 * Custom serializer for workspaces that can be either an array or an object.
 */
private object WorkspacesConfigSerializer : kotlinx.serialization.KSerializer<WorkspacesConfig> {
  override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("WorkspacesConfig")

  override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: WorkspacesConfig) {
    error("Serialization not supported")
  }

  override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): WorkspacesConfig {
    val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder
      ?: error("Can only deserialize from JSON")

    val element = jsonDecoder.decodeJsonElement()
    return when {
      element is kotlinx.serialization.json.JsonArray -> {
        val packages = element.map { it.jsonPrimitive.content }
        WorkspacesConfig.Array(packages)
      }
      element is kotlinx.serialization.json.JsonObject -> {
        val packagesArray = element["packages"] as? kotlinx.serialization.json.JsonArray
          ?: error("Expected 'packages' array in workspaces object")
        val packages = packagesArray.map { it.jsonPrimitive.content }
        WorkspacesConfig.Object(packages)
      }
      else -> error("Expected array or object for workspaces")
    }
  }
}

/**
 * Parser for Node.js package.json files.
 *
 * Extracts project information including:
 * - Package metadata (name, version, description)
 * - Dependencies (dependencies, devDependencies, peerDependencies, optionalDependencies)
 * - Workspaces configuration (for monorepos)
 * - NPM scripts
 */
internal object PackageJsonParser {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Parse a package.json file.
   *
   * @param packageJsonPath Path to package.json
   * @return Parsed package.json descriptor
   */
  fun parse(packageJsonPath: Path): PackageJsonDescriptor {
    if (!packageJsonPath.exists()) {
      throw IllegalArgumentException("package.json not found: $packageJsonPath")
    }

    val content = packageJsonPath.readText()
    val packageJson = json.decodeFromString<PackageJson>(content)

    return PackageJsonDescriptor(
      name = packageJson.name,
      version = packageJson.version,
      description = packageJson.description,
      dependencies = packageJson.dependencies ?: emptyMap(),
      devDependencies = packageJson.devDependencies ?: emptyMap(),
      peerDependencies = packageJson.peerDependencies ?: emptyMap(),
      optionalDependencies = packageJson.optionalDependencies ?: emptyMap(),
      workspaces = packageJson.workspaces?.packages() ?: emptyList(),
      scripts = packageJson.scripts ?: emptyMap(),
      packageJsonPath = packageJsonPath,
    )
  }
}
