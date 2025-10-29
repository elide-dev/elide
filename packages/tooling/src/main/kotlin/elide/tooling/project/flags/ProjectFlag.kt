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

import elide.annotations.API

/**
 * # Project Flag
 *
 * Defines flags which exist within the context of a specific Elide project; flags can be used to create conditional
 * state within a project's build file, or pass inputs which are used within the project's scripts.
 *
 * Project flags are always defined explicitly, values for which are provided during the evaluation stage of the project
 * `elide.pkl` file(s).
 */
@API public sealed interface ProjectFlag {
  public val key: ProjectFlagKey
  public val value: ProjectFlagValue

  public val asString: String get() = value.asString

  /**
   * ## Project Flag: Keyed
   *
   * Holds a [key] and associated [value] as a project flag entry.
   */
  @JvmRecord public data class KeyedFlag(
    override val key: ProjectFlagKey,
    override val value: ProjectFlagValue,
  ) : ProjectFlag

  public companion object {
    @JvmStatic public fun of(key: ProjectFlagKey, value: ProjectFlagValue): ProjectFlag = KeyedFlag(
      key = key,
      value = value,
    )
  }
}
