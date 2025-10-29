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

/**
 * ## Project Flag Key
 *
 * Describes a key which carries an identity for a given "project" flag; such flags are defined explicitly within the
 * scope of a given Elide project. See [ProjectFlag].
 *
 * @see ProjectFlag concept of project flags
 */
public sealed interface ProjectFlagKey: Comparable<ProjectFlagKey> {
  /**
   * Name of this flag without any preceding flag symbols.
   */
  public val strippedName: String

  override fun compareTo(other: ProjectFlagKey): Int {
    return strippedName.compareTo(other.strippedName)
  }

  @JvmInline private value class StringFlagKey private constructor (public val name: String): ProjectFlagKey {
    override val strippedName: String get() = name.replace("--", "").replace("-", "")

    companion object {
      @JvmStatic fun of(name: String): ProjectFlagKey = StringFlagKey(name.trim())
    }
  }

  public companion object {
    @JvmStatic public fun of(name: String): ProjectFlagKey = StringFlagKey.of(name)
  }
}
