/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("DataClassPrivateConstructor")

package elide.tool.project

import java.util.SortedMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.runtime.plugins.env.EnvConfig.EnvVar

/**
 * TBD.
 */
@JvmInline value class ProjectInfo private constructor (private val info: ElideProject) {
  /** Environment settings applied to the project. */
  @JvmRecord @Serializable data class ProjectEnvironment private constructor (
    @Transient val vars: SortedMap<String, EnvVar> = sortedMapOf(),
  ) {
    companion object {
      /** @return Project environment wrapping the provided [map] of env vars. */
      @JvmStatic fun wrapping(map: SortedMap<String, EnvVar>) = ProjectEnvironment(vars = map)
    }
  }

  /** Information about an Elide project. */
  @JvmRecord @Serializable internal data class ElideProject (
    val name: String? = null,
    val root: String? = null,
    val version: String? = null,
    val env: ProjectEnvironment? = null,
    val config: ProjectConfig? = null,
  )

  /** Return the name of this project. */
  val name: String? get() = info.name

  /** Return the version assigned currently for this project. */
  val version: String? get() = info.version ?: info.config?.version

  /** Return the root path of this project (absolute). */
  val root: String? get() = info.root

  /** Return the effective project configuration. */
  val config: ProjectConfig? get() = info.config

  /** Return project environment settings. */
  val env: ProjectEnvironment? get() = info.env

  companion object {
    /** @return Wrapped [ProjectInfo] describing the provided inputs. */
    @JvmStatic fun of(
      root: String,
      name: String? = null,
      env: ProjectEnvironment? = null,
      config: ProjectConfig? = null,
    ): ProjectInfo = ProjectInfo(ElideProject(
      name = name,
      root = root,
      env = env,
      config = config,
    ))
  }
}
