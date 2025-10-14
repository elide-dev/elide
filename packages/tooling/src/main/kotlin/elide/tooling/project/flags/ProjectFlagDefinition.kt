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
package elide.tooling.project.flags

import kotlinx.serialization.Serializable

/**
 * ## Project Flag Definition
 *
 * Defines the structure, type, and identity, of a [ProjectFlag], which is constituent to a given Elide project; the
 * flag's definition is responsible for matching the flag during parsing, and for performing any processing required to
 * extract a flag value.
 *
 * The flag's "schema," so to speak, can be interrogated using the flag's definition.
 *
 * @property name Name of the flag under definition.
 * @property aliases Alternate names which should match this flag.
 * @property description Optional human-readable description of the flag.
 * @property type Type of data produced by the project flag.
 * @property required Whether to require a value for this flag.
 * @property defaultValue Default value assigned to this flag, if any.
 */
@Serializable public class ProjectFlagDefinition private constructor (
  private val info: FlagInfo,
) {
  // Private flag info structure.
  @Serializable @JvmRecord public data class FlagInfo(
    val name: String,
    val aliases: List<String>,
    val description: String? = null,
    val type: ProjectFlagType,
    val required: Boolean,
    val defaultValue: ProjectFlagValue = ProjectFlagValue.NoValue,
  )

  public val name: String get() = info.name
  public val aliases: List<String> get() = info.aliases
  public val description: String? get() = info.description
  public val type: ProjectFlagType get() = info.type
  public val required: Boolean get() = info.required
  public val defaultValue: ProjectFlagValue get() = info.defaultValue

  /** Static utilities for [ProjectFlagDefinition]. */
  public companion object {
    /**
     * Create a new project flag definition.
     *
     * @param name Name of the flag.
     * @param aliases Alternate names for the flag.
     * @param description Human-readable description for the flag.
     * @param type Type of data held by the flag.
     * @param required Whether the flag is required.
     * @param defaultValue Default value for the flag, if any.
     * @return Flag definition record.
     */
    @JvmStatic public fun of(
      name: String,
      aliases: List<String>? = null,
      description: String? = null,
      type: ProjectFlagType = ProjectFlagType.STRING,
      required: Boolean = false,
      defaultValue: ProjectFlagValue = ProjectFlagValue.NoValue,
    ): ProjectFlagDefinition = ProjectFlagDefinition(
      FlagInfo(
        name = name,
        aliases = aliases ?: emptyList(),
        description = description,
        type = type,
        required = required,
        defaultValue = defaultValue,
      )
    )

    /** @return Flag definition from a decoded [FlagInfo]. */
    @JvmStatic public fun from(model: FlagInfo): ProjectFlagDefinition = ProjectFlagDefinition(model)
  }
}
