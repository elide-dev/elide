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
package elide.versions.repository

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Sink
import elide.versions.ElideInstallEvent
import elide.versions.ElideVersionDto

/**
 * Abstract class for standard [repositories][ElideRepository]. A standard repository has an [ElideVersionCatalog] at
 * [catalogPath].
 *
 * @author Lauri Heino <datafox>
 */
internal abstract class StandardElideRepository(val catalogPath: String) : ElideRepository {
  /** Returns the [ElideVersionCatalog] at [catalogPath]. */
  protected abstract suspend fun getVersionCatalog(): ElideVersionCatalog

  /** Streams a file from [path] to [sink] while emitting [progress] updates. */
  protected abstract suspend fun streamFile(path: String, sink: Sink, progress: FlowCollector<ElideInstallEvent>?)

  override suspend fun getVersions(): List<ElideVersionDto> =
    getVersionCatalog().versions.flatMap { (version, systems) ->
      systems.platforms.map { (platform, _) -> ElideVersionDto(version, platform) }
    }

  override suspend fun getFile(
    version: ElideVersionDto,
    extension: String,
    sink: Sink,
    progress: FlowCollector<ElideInstallEvent>?,
  ) {
    val versionData =
      requireNotNull(getVersionCatalog().versions[version.version]) {
        "The version ${version.version} does not exist in repository"
      }
    val path =
      requireNotNull(versionData.platforms[version.platform]) {
        "The version ${version.version} does not exist in repository"
      }
    streamFile("$path.$extension", sink, progress)
  }
}
