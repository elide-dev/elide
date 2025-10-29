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

import elide.struct.TreeMap
import elide.tooling.project.ProjectManager

/**
 * ## Project Flags Context
 *
 * Created within the scope of a given Elide project to hold project flag definitions, and to hold their keys and values
 * once such definitions have been parsed.
 */
public class ProjectFlagsContext private constructor (private val flagMap: TreeMap<ProjectFlagKey, ProjectFlag>) {
  public companion object {
    // Immutable empty flag context singleton.
    public val EMPTY: ProjectFlagsContext = ProjectFlagsContext(TreeMap())

    @JvmStatic public fun from(params: ProjectManager.ProjectParams): ProjectFlagsContext = from(
      params.flags.toMap()
    )

    @JvmStatic public fun from(flagMap: Map<ProjectFlagKey, ProjectFlagValue>): ProjectFlagsContext = create(
      TreeMap<ProjectFlagKey, ProjectFlag>().also {
        flagMap.forEach { (key, value) ->
          it[key] = ProjectFlag.of(key, value)
        }
      }
    )

    @JvmStatic public fun create(flags: TreeMap<ProjectFlagKey, ProjectFlag>): ProjectFlagsContext =
      ProjectFlagsContext(flags)
  }

  public operator fun contains(key: String): Boolean {
    return contains(ProjectFlagKey.of(key))
  }

  public operator fun contains(key: ProjectFlagKey): Boolean {
    return flagMap.containsKey(key)
  }

  public operator fun get(key: ProjectFlagKey): ProjectFlag {
    return flagMap[key] ?: ProjectFlag.of(key, ProjectFlagValue.NoValue)
  }
}
