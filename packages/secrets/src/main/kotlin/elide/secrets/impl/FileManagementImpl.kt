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
package elide.secrets.impl

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import elide.annotations.Singleton
import elide.secrets.*
import elide.secrets.SecretUtils.decrypt
import elide.secrets.SecretUtils.deserialize
import elide.secrets.SecretUtils.encrypt
import elide.secrets.SecretUtils.exists
import elide.secrets.SecretUtils.read
import elide.secrets.SecretUtils.serialize
import elide.secrets.SecretUtils.write
import elide.secrets.dto.persisted.*

/**
 * Implementation of [FileManagement].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class FileManagementImpl(
  private val encryption: Encryption,
  private val json: Json,
  private val cbor: BinaryFormat,
) : FileManagement {
  override fun metadataExists(): Boolean = localMetadataPath().exists()

  override fun readMetadata(): LocalMetadata = localMetadataPath().read().deserialize(json)

  override fun writeMetadata(): ByteString = SecretsState.metadata.serialize(json).write(localMetadataPath())

  override fun localExists(): Boolean = localProfilePath().exists()

  override fun readLocal(): LocalProfile =
    localProfilePath().read().decrypt(SecretsState.userKey, encryption).deserialize(cbor)

  override fun writeLocal(): ByteString =
    SecretsState.local.serialize(cbor).encrypt(SecretsState.userKey, encryption).write(localProfilePath())

  override fun canDecryptLocal(passphrase: String): Boolean {
    try {
      localProfilePath()
        .read()
        .decrypt(UserKey(encryption.hashKeySHA256(passphrase.encodeToByteString())), encryption)
        .deserialize<LocalProfile>(cbor)
    } catch (_: SerializationException) {
      return false
    }
    return true
  }

  override fun profileExists(profile: String): Boolean = profilePath(profile).exists()

  override fun readProfile(profile: String): Pair<SecretProfile, SecretKey> {
    val key = readKey(profile)
    val profile: SecretProfile = profilePath(profile).read().decrypt(key, encryption).deserialize(cbor)
    return profile to key
  }

  override fun writeProfile(profile: SecretProfile, key: SecretKey): ByteString {
    writeKey(key)
    return profile.serialize(cbor).encrypt(key, encryption).write(profilePath(profile.name))
  }

  override fun writeProfileBytes(profile: String, data: ByteString) {
    data.write(profilePath(profile))
  }

  override fun writeKey(key: SecretKey) {
    key.serialize(cbor).encrypt(SecretsState.userKey, encryption).write(keyPath(key.name))
  }

  override fun removeProfile(profile: String) {
    SystemFileSystem.delete(profilePath(profile))
    SystemFileSystem.delete(keyPath(profile))
  }

  override fun profileBytes(profile: String): ByteString = profilePath(profile).read()

  override fun readKey(profile: String): SecretKey =
    keyPath(profile).read().decrypt(SecretsState.userKey, encryption).deserialize(cbor)

  private fun localMetadataPath(): Path = Path(SecretsState.path, SecretValues.METADATA_FILE)

  private fun localProfilePath(): Path = Path(SecretsState.path, SecretValues.LOCAL_FILE)

  private fun profilePath(profile: String): Path = Path(SecretsState.path, SecretUtils.profileName(profile))

  private fun keyPath(profile: String): Path = Path(SecretsState.path, SecretUtils.keyName(profile))
}
