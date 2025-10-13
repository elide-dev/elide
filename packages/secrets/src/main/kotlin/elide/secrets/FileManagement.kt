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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.FileNotFoundException
import elide.secrets.dto.persisted.LocalMetadata
import elide.secrets.dto.persisted.LocalProfile
import elide.secrets.dto.persisted.SecretKey
import elide.secrets.dto.persisted.SecretProfile

/**
 * Local secret file operations.
 *
 * @author Lauri Heino <datafox>
 */
internal interface FileManagement {
  /** Returns true if the metadata file exists. */
  fun metadataExists(): Boolean

  /** Reads local metadata or throws [FileNotFoundException] if the file does not exist. */
  @Throws(FileNotFoundException::class) fun readMetadata(): LocalMetadata

  /** Writes local metadata from [SecretsState], creating a new file if one does not exist, and returns its bytes. */
  fun writeMetadata(): ByteString

  /** Returns `true` if the local secrets file exists. */
  fun localExists(): Boolean

  /** Reads local secrets or throws [FileNotFoundException] if the file does not exist. */
  @Throws(FileNotFoundException::class) fun readLocal(): LocalProfile

  /** Writes local secrets from [SecretsState], creating a new file if one does not exist, and returns its bytes. */
  fun writeLocal(): ByteString

  /**
   * Reads the local secrets file or throws [FileNotFoundException] if the file does not exist, then returns `true` if
   * the file can be decrypted with [passphrase].
   */
  @Throws(FileNotFoundException::class) fun canDecryptLocal(passphrase: String): Boolean

  /** Returns `true` if a file for [profile] exists. */
  fun profileExists(profile: String): Boolean

  /** Reads a [profile] and its key or throws [FileNotFoundException] if either of the files does not exist. */
  @Throws(FileNotFoundException::class) fun readProfile(profile: String): Pair<SecretProfile, SecretKey>

  /** Writes [profile] and [key], creating new files if they do not exist, and returns the profile's bytes. */
  fun writeProfile(profile: SecretProfile, key: SecretKey): ByteString

  /** Writes [data] to the file of [profile], creating a new file if one does not exist. */
  fun writeProfileBytes(profile: String, data: ByteString)

  /** Writes [key], creating a new file if one does not exist. */
  fun writeKey(key: SecretKey)

  /** Deletes [profile] and its key or throws [FileNotFoundException] if either of the files does not exist. */
  @Throws(FileNotFoundException::class) fun removeProfile(profile: String)

  /** Reads bytes of a [profile] or throws [FileNotFoundException] if the file does not exist. */
  @Throws(FileNotFoundException::class) fun profileBytes(profile: String): ByteString

  /** Reads [profile]'s or throws [FileNotFoundException] if the file does not exist. */
  @Throws(FileNotFoundException::class) fun readKey(profile: String): SecretKey
}
