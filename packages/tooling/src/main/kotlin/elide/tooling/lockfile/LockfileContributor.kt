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
package elide.tooling.lockfile

import java.nio.file.Path
import elide.tooling.project.ElideProject

/**
 * # Lockfile Contributor
 *
 * Describes an implementation which contributes data to an [ElideLockfile].
 */
public fun interface LockfileContributor {
  /**
   * Contribute a stanza to the lockfile.
   *
   * If the contributor does not have any data to contribute, it should return `null`, in which case the lockfile will
   * not include any data from the contributor.
   *
   * @param root Root path where lockfile building is taking place.
   * @param project Project being built.
   * @return Stanza to contribute to the lockfile, or `null` if there is no data to contribute.
   */
  public suspend fun contribute(root: Path, project: ElideProject?): ElideLockfile.Stanza?
}
