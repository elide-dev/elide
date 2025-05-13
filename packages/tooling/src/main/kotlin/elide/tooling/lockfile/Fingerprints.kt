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
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import elide.tooling.lockfile.ElideLockfile.*
import elide.tooling.project.ElideProject

/**
 * # Fingerprints
 *
 * Static utilities which aid in the construction of [Fingerprint] instances, which are used within Elide Project
 * lock files.
 */
public object Fingerprints {
  /**
   * ## Build From Stanzas
   *
   * Build a fingerprint from the given stanzas.
   *
   * @param stanzas Stanzas to build a fingerprint from.
   * @return Fingerprint built from the given stanzas.
   */
  @JvmStatic public fun buildFrom(stanzas: List<Stanza>): Fingerprint {
    val all = stanzas.map { it.fingerprint }
    return Fingerprint.of(all)
  }

  /**
   * ## Build From Project
   *
   * Build a fingerprint from the given project.
   *
   * @param project Project to build a fingerprint from.
   * @return Fingerprint built from the given project.
   */
  @JvmStatic public fun forProject(project: ElideProject): Fingerprint {
    // we use a hash-code here because project manifests are well-formed data objects
    return Fingerprint.of(project.manifest.hashCode().toLong())
  }

  /**
   * ## Build From Bytes
   *
   * Fingerprint value from raw [bytes].
   *
   * @param bytes Raw bytes to build a fingerprint from.
   * @return Fingerprint built from the given bytes.
   */
  @JvmStatic public fun forBytes(bytes: ByteArray): Fingerprint {
    return Fingerprint.Bytes.of(bytes)
  }

  /**
   * ## Build From File
   *
   * Build a fingerprint from the given file path.
   *
   * @param path Path to the file to build a fingerprint from.
   * @return Fingerprint built from the given file path.
   */
  @JvmStatic public fun forFile(path: Path, digest: Boolean = false): Fingerprint = when {
    !path.exists() -> Fingerprint.NoContent
    !path.isRegularFile() -> error("Directory or other non-regular file passed to `Fingerprints.forFile`: $path")
    else -> if (digest) {
      Fingerprint.ofFileDigest(path.toFile())
    } else {
      Fingerprint.ofFileState(path.toFile())
    }
  }
}
