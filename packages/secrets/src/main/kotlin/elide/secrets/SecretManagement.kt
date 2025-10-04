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
package elide.secrets

import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Interactive read/write access to secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface SecretManagement : SecretsCommon {
  /** Initializes secrets state non-interactively using a single profile pulled from a remote. */
  public suspend fun initNonInteractive(path: Path, manifest: ElidePackageManifest)

  /** Loads local secrets for editing. */
  public fun loadLocalProfile()

  /** Creates a new profile. */
  public fun createProfile(profile: String)

  /** Deletes a profile. */
  public fun deleteProfile(profile: String)

  /** Creates or replaces a text secret in the loaded profile. */
  public fun setTextSecret(name: String, value: String, envVar: String? = null)

  /**
   * Creates or updates a text secret in the loaded profile. Unlike [setTextSecret], this preserves the current
   * environment variable name if set.
   */
  public fun updateTextSecret(name: String, value: String)

  /** Creates or replaces a binary secret in the loaded profile. */
  public fun setBinarySecret(name: String, value: ByteString)

  /** Removes a secret from the loaded profile. */
  public fun removeSecret(name: String)

  /** Writes changes to the loaded profile. */
  public fun writeChanges()

  /** Prompts user on a new encryption mode and re-encrypts local secrets and key files. */
  public fun changeEncryption()

  /**
   * Pulls changes from a remote. Only works if the current secrets have been initialized from a remote as a normal
   * user.
   */
  public suspend fun pullFromRemote()

  /**
   * Pushes changes to a remote. Only works if the current secrets have been initialized from a remote as a normal user.
   */
  public suspend fun pushToRemote()

  /** Returns a [RemoteManagement] for managing remote secrets as a superuser. */
  public suspend fun manageRemote(): RemoteManagement

  /** Creates or replaces a binary secret in the loaded profile. */
  public fun setBinarySecret(name: String, value: ByteArray): Unit = setBinarySecret(name, ByteString(value))
}
