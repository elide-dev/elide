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
package elide.tooling.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.runtime.EnvVar

/** Environment settings applied to the project. */
@JvmRecord @Serializable
public data class ProjectEnvironment private constructor(
  @Transient public val vars: Map<String, EnvVar> = sortedMapOf(),
) {
  public companion object {
    /** @return Project environment wrapping the provided [map] of env vars. */
    @JvmStatic public fun wrapping(map: Map<String, EnvVar>): ProjectEnvironment = ProjectEnvironment(vars = map)
  }

  public operator fun plus(other: ProjectEnvironment): ProjectEnvironment {
    return ProjectEnvironment(vars + other.vars)
  }
}
