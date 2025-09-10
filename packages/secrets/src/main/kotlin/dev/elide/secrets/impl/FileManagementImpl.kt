package dev.elide.secrets.impl

import dev.elide.secrets.Encryption
import dev.elide.secrets.FileManagement
import dev.elide.secrets.SecretsState
import dev.elide.secrets.Utils
import dev.elide.secrets.Utils.decrypt
import dev.elide.secrets.Utils.deserialize
import dev.elide.secrets.Utils.encrypt
import dev.elide.secrets.Utils.exists
import dev.elide.secrets.Utils.read
import dev.elide.secrets.Utils.serialize
import dev.elide.secrets.Utils.write
import dev.elide.secrets.Values
import dev.elide.secrets.dto.persisted.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import elide.annotations.Singleton

/** @author Lauri Heino <datafox> */
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

  override fun writeProfile(profile: SecretProfile, key: SecretKey): Pair<ByteString, ByteString> {
    val profileBytes = profile.serialize(cbor).encrypt(key, encryption).write(profilePath(profile.name))
    val keyBytes = key.serialize(cbor).encrypt(SecretsState.userKey, encryption).write(keyPath(profile.name))
    return profileBytes to keyBytes
  }

  override fun removeProfile(profile: String) {
    SystemFileSystem.delete(profilePath(profile))
    SystemFileSystem.delete(keyPath(profile))
  }

  override fun profileBytes(profile: String): ByteString = profilePath(profile).read()

  override fun readKey(profile: String): SecretKey =
    keyPath(profile).read().decrypt(SecretsState.userKey, encryption).deserialize(cbor)

  private fun localMetadataPath(): Path = Path(SecretsState.path, Values.METADATA_FILE)

  private fun localProfilePath(): Path = Path(SecretsState.path, Values.LOCAL_FILE)

  private fun profilePath(profile: String): Path =
    Path(SecretsState.path, Utils.profileName(profile))

  private fun keyPath(profile: String): Path =
    Path(SecretsState.path, Utils.keyName(profile))
}
