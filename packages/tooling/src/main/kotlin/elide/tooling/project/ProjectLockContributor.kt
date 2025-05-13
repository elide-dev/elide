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

import java.nio.file.Path
import elide.tooling.lockfile.ElideLockfile.*
import elide.tooling.lockfile.Fingerprints
import elide.tooling.lockfile.LockfileContributor

// Contributes Elide project state to the lockfile.
internal class ProjectLockContributor : LockfileContributor {
  override suspend fun contribute(root: Path, project: ElideProject?): Stanza? {
    val digest = Fingerprints.forFile(root.resolve("elide.pkl"), digest = true)
    return project?.let {
      StanzaData(
        identifier = "project",
        fingerprint = digest,
        contributedBy = "elide",
        inputs = sortedSetOf(InputMaterial.DependencyManifest.of(
          ecosystem = ProjectEcosystem.Elide,
          identifier = "elide.pkl",
          fingerprint = digest,
        ))
      )
    }
  }
}
