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
package elide.tooling.js.resolver

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.relativeTo
import elide.tooling.lockfile.ElideLockfile.*
import elide.tooling.lockfile.ElideLockfile.InputMaterial.PeerLockfile
import elide.tooling.lockfile.Fingerprints
import elide.tooling.lockfile.LockfileContributor
import elide.tooling.project.ElideProject

// Contributes NPM lockfile state to the Elide lockfile.
internal class NpmLockContributor : LockfileContributor {
  override suspend fun contribute(root: Path, project: ElideProject?): Stanza? {
    return listOf<Path>(
      root.resolve("package-lock.kdl"),
      root.resolve("pnpm-lock.yaml"),
      root.resolve("yarn.lock"),
      root.resolve("package-lock.json"),
    ).find {
      it.exists()
    }?.let { npmManifestLock ->
      PeerLockfile.of(
        identifier = npmManifestLock.relativeTo(root).toString(),
        fingerprint = Fingerprints.forFile(npmManifestLock, digest = true),
        remarks = Remarks.text("NPM lockfile"),
        generatedBy = when (npmManifestLock.fileName.toString()) {
          "package-lock.kdl" -> "orogene"
          "pnpm-lock.yaml" -> "pnpm"
          "yarn.lock" -> "yarn"
          "package-lock.json" -> "npm"
          else -> "Unknown"
        },
      )
    }?.let { inputMaterial ->
      StanzaData(
        identifier = "npm",
        fingerprint = inputMaterial.fingerprint,
        contributedBy = "NPM build integration",
        inputs = sortedSetOf(inputMaterial),
      )
    }
  }
}
