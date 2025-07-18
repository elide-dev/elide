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
package elide.tool.server

import java.nio.file.Path
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.path.absolutePathString
import elide.tooling.project.ElideProject

// Well-known properties recognized by Chrome.
private const val WORKSPACE_ROOT = "root"
private const val WORKSPACE_UUID = "uuid"

/**
 * ## Chrome Devtools Workspace
 *
 * @property workspace Workspace information, including root and UUID.
 */
@JvmRecord @Serializable data class ChromeDevtoolsWorkspace private constructor (
  val workspace: Workspace,
) {
  /**
   * ### Chrome Devtools: Workspace Information
   *
   * Holds inner workspace information, including the root path and UUID.
   *
   * @property root The root path of the workspace.
   * @property uuid The unique identifier for the workspace.
   */
  @JvmRecord @Serializable data class Workspace private constructor (
    @SerialName(WORKSPACE_ROOT) val root: String,
    @SerialName(WORKSPACE_UUID) val uuid: String,
  ) {
    internal companion object {
      @JvmStatic fun of(root: String, uuid: String): Workspace = Workspace(
        root = root,
        uuid = uuid,
      )
    }
  }

  /** Methods for building or obtaining instances of [ChromeDevtoolsWorkspace]. */
  companion object {
    /**
     * Build a [ChromeDevtoolsWorkspace] for the given [path] and [uuid].
     *
     * Note: The provided [path] must be absolute; if it is not absolute, it will be resolved.
     *
     * @param path The path to the workspace root.
     * @param uuid The unique identifier for the workspace.
     * @return A new instance of [ChromeDevtoolsWorkspace] with the specified path and UUID.
     */
    @JvmStatic fun buildFor(path: Path, uuid: UUID? = null): ChromeDevtoolsWorkspace = ChromeDevtoolsWorkspace(
      workspace = Workspace.of(
        root = path.absolutePathString(),
        uuid = (uuid ?: UUID.randomUUID()).toString(),
      )
    )

    /**
     * Build a [ChromeDevtoolsWorkspace] for the given [ElideProject] and an optional [uuid].
     *
     * If a UUID is not provided, a new one will be generated.
     *
     * @param project The [ElideProject] for which to create the workspace.
     * @param uuid An optional UUID for the workspace; if not provided, a new UUID will be generated.
     * @return A new instance of [ChromeDevtoolsWorkspace] for the specified project.
     */
    @JvmStatic fun forProject(project: ElideProject, uuid: UUID? = null): ChromeDevtoolsWorkspace = buildFor(
      path = project.root,
      uuid = (uuid ?: UUID.randomUUID()),
    )
  }
}
