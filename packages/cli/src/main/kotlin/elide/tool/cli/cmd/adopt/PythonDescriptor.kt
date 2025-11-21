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

/**
 * Python project descriptor parsed from various Python configuration files.
 *
 * Supports multiple Python project formats:
 * - pyproject.toml (PEP 621)
 * - requirements.txt
 * - Pipfile
 * - setup.py (limited)
 *
 * @property name Project name
 * @property version Project version
 * @property description Project description
 * @property pythonVersion Python version requirement (e.g., ">=3.11")
 * @property dependencies Production dependencies with version specifiers
 * @property devDependencies Development dependencies
 * @property optionalDependencies Optional dependencies grouped by extra name
 * @property scripts Entry points/console scripts
 * @property buildSystem Build backend (e.g., "setuptools.build_meta")
 * @property configPath Path to the configuration file
 * @property sourceType Type of source configuration
 */
public data class PythonDescriptor(
  val name: String,
  val version: String? = null,
  val description: String? = null,
  val pythonVersion: String? = null,
  val dependencies: List<String> = emptyList(),
  val devDependencies: List<String> = emptyList(),
  val optionalDependencies: Map<String, List<String>> = emptyMap(),
  val scripts: Map<String, String> = emptyMap(),
  val buildSystem: String? = null,
  val configPath: Path? = null,
  val sourceType: SourceType = SourceType.PYPROJECT_TOML,
) {
  /**
   * Type of Python configuration source.
   */
  enum class SourceType {
    PYPROJECT_TOML,
    REQUIREMENTS_TXT,
    PIPFILE,
    SETUP_PY,
  }

  /**
   * Get all dependencies (production + dev).
   */
  fun allDependencies(): List<String> {
    return dependencies + devDependencies
  }

  /**
   * Get all optional dependencies flattened.
   */
  fun allOptionalDependencies(): List<String> {
    return optionalDependencies.values.flatten()
  }

  /**
   * Check if this project has development dependencies.
   */
  fun hasDevDependencies(): Boolean {
    return devDependencies.isNotEmpty()
  }

  /**
   * Check if this project has optional dependencies.
   */
  fun hasOptionalDependencies(): Boolean {
    return optionalDependencies.isNotEmpty()
  }

  /**
   * Check if this project has entry points/scripts.
   */
  fun hasScripts(): Boolean {
    return scripts.isNotEmpty()
  }

  /**
   * Get the display name for the source type.
   */
  fun sourceDisplayName(): String = when (sourceType) {
    SourceType.PYPROJECT_TOML -> "pyproject.toml"
    SourceType.REQUIREMENTS_TXT -> "requirements.txt"
    SourceType.PIPFILE -> "Pipfile"
    SourceType.SETUP_PY -> "setup.py"
  }
}

/**
 * Python dependency with version specifier.
 *
 * Examples:
 * - "fastapi>=0.104.0"
 * - "uvicorn[standard]>=0.24.0"
 * - "pytest==7.4.0"
 * - "requests~=2.31.0"
 *
 * @property raw Raw dependency string
 */
internal data class PythonDependency(val raw: String) {
  /**
   * Package name (without extras or version).
   */
  val packageName: String by lazy {
    raw.split("[")[0].split(">=")[0].split("==")[0].split("~=")[0].split("!=")[0]
      .split(">")[0].split("<")[0].trim()
  }

  /**
   * Extras (e.g., "standard" from "uvicorn[standard]").
   */
  val extras: List<String> by lazy {
    val match = Regex("""\[([^\]]+)]""").find(raw)
    match?.groupValues?.get(1)?.split(",")?.map { it.trim() } ?: emptyList()
  }

  /**
   * Version specifier (e.g., ">=0.104.0").
   */
  val versionSpecifier: String? by lazy {
    val withoutExtras = if (raw.contains("[")) {
      raw.substring(raw.indexOf("]") + 1)
    } else {
      raw.substring(packageName.length)
    }.trim()

    if (withoutExtras.isEmpty()) null else withoutExtras
  }

  /**
   * Check if this dependency has extras.
   */
  fun hasExtras(): Boolean = extras.isNotEmpty()

  /**
   * Format with extras.
   */
  fun withExtras(): String {
    return if (hasExtras()) {
      "$packageName[${extras.joinToString(",")}]${versionSpecifier ?: ""}"
    } else {
      raw
    }
  }

  override fun toString(): String = raw
}
