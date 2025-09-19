package dev.elide.secrets.impl

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptCheckbox
import dev.elide.secrets.*
import dev.elide.secrets.Utils.decrypt
import dev.elide.secrets.Utils.deserialize
import dev.elide.secrets.Utils.encrypt
import dev.elide.secrets.Utils.serialize
import dev.elide.secrets.dto.persisted.*
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.json.Json

internal class RemoteManagementImpl(
  private val secrets: Secrets,
  private val files: FileManagement,
  private val encryption: Encryption,
  private val json: Json,
  private val cbor: BinaryFormat,
  private val remoteMetadata: RemoteMetadata,
  private var superAccess: SuperAccess,
  private var superKey: UserKey,
) : RemoteManagement {
  private var access: String? = null

  override suspend fun init() {
    val localProfiles = secrets.listProfiles()
    val remoteProfiles = remoteMetadata.profiles.keys
    val localAndRemote = localProfiles intersect remoteProfiles
    val mismatchingHashes =
      localAndRemote.filter {
        SecretsState.metadata.profiles[it]!!.hash != remoteMetadata.profiles[it]!!.hash ||
                SecretsState.metadata.profiles[it]!!.keyHash != remoteMetadata.profiles[it]!!.keyHash
      }
    val updated = if (mismatchingHashes.isEmpty()) listOf() else KInquirer.promptCheckbox(
      "These profiles exist locally but are not the same ones as on the remote. Select the local profiles you want to update from the remote, the rest will be pushed to the remote.",
      mismatchingHashes,
    )
    val localToBeUpdated = (remoteProfiles subtract localProfiles) union updated
    localToBeUpdated.forEach {
      val key = superAccess.keys[it]!!
      val profile: SecretProfile = SecretsState.remote.getProfile(it)!!.decrypt(key, encryption).deserialize(cbor)
      files.writeProfile(profile, key)
      SecretsState.updateMetadata { add(remoteMetadata.profiles[it]!!) }
    }
    if (localToBeUpdated.isNotEmpty()) files.writeMetadata()
    superAccess = superAccess.copy(keys = superAccess.keys + localProfiles.associateWith { files.readKey(it) })
  }

  override fun listAccesses(): Set<String> = superAccess.access.keys

  override fun createAccess(name: String) {
    val mode = Prompts.accessMode()
    superAccess =
      superAccess.addAccess(
        name,
        when (mode) {
          EncryptionMode.PASSPHRASE -> UserKey(encryption.hashKeySHA256(Prompts.passphrase().encodeToByteString()))
          EncryptionMode.GPG -> UserKey(Prompts.gpgPublicKey())
        },
      )
  }

  override fun removeAccess(name: String) {
    superAccess = superAccess.removeAccess(name)
  }

  override fun selectAccess(name: String) {
    if (name !in superAccess.access) throw IllegalArgumentException("Access $access does not exist.")
    access = name
  }

  override fun addProfile(profile: String) {
    if (access == null) throw IllegalStateException("No access is selected.")
    access?.let { superAccess = superAccess.addToAccess(it, profile) }
  }

  override fun removeProfile(profile: String) {
    if (access == null) throw IllegalStateException("No access is selected.")
    access?.let { superAccess = superAccess.removeFromAccess(it, profile) }
  }

  override fun listProfiles(): Set<String> {
    if (access == null) throw IllegalStateException("No access is selected.")
    return superAccess.access[access!!]!!.second
  }

  override fun deselectAccess() {
    access = null
  }

  override suspend fun push() {
    val superBytes = superAccess.serialize(cbor).encrypt(superKey, encryption)
    val accesses = superAccess.access.mapValues {
      SecretAccess(it.key, it.value.second.associateWith { profile -> files.readKey(profile) })
        .serialize(cbor)
        .encrypt(it.value.first, encryption)
    }
    val newRemoteMetadata = remoteMetadata.copy(
      profiles = remoteMetadata.profiles + SecretsState.metadata.profiles,
      superAccess = AccessMetadata("super", encryption.hashGitDataSHA1(superBytes), superKey),
      access = accesses.map { AccessMetadata(it.key, encryption.hashGitDataSHA1(it.value), superAccess.access[it.key]!!.first) }.associateBy { it.name }
    )
    SecretsState.remote.superUpdate(
      newRemoteMetadata.serialize(json),
      newRemoteMetadata.profiles
        .filter {
          it.key !in remoteMetadata.profiles ||
                  it.value.hash != remoteMetadata.profiles[it.key]!!.hash ||
                  it.value.keyHash != remoteMetadata.profiles[it.key]!!.keyHash
        }
        .mapValues { files.profileBytes(it.key) },
      superBytes,
      accesses,
    )
  }
}
