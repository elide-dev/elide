/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.runtime.plugins.env.EnvConfig.EnvVar
import elide.tool.project.manifest.ElidePackageManifest

/** Information about an Elide project. */
@JvmRecord @Serializable internal data class ElideProject(
  val root: Path,
  val manifest: ElidePackageManifest,
  val env: ProjectEnvironment? = null,
)

/** Environment settings applied to the project. */
@JvmRecord @Serializable data class ProjectEnvironment private constructor(
  @Transient val vars: Map<String, EnvVar> = sortedMapOf(),
) {
  companion object {
    /** @return Project environment wrapping the provided [map] of env vars. */
    @JvmStatic fun wrapping(map: Map<String, EnvVar>) = ProjectEnvironment(vars = map)
  }
}
