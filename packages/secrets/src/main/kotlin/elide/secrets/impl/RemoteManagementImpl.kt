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

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptCheckbox
import com.github.kinquirer.components.promptConfirm
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.json.Json
import elide.secrets.*
import elide.secrets.Utils.decrypt
import elide.secrets.Utils.deserialize
import elide.secrets.Utils.encrypt
import elide.secrets.Utils.serialize
import elide.secrets.dto.persisted.*

internal class RemoteManagementImpl(
  private val secrets: Secrets,
  private val files: FileManagement,
  private val encryption: Encryption,
  private val json: Json,
  private val cbor: BinaryFormat,
  private val remoteMetadata: RemoteMetadata,
  private var superAccess: SuperAccess,
  private var superKey: UserKey,
  private val prompts: MutableList<String>,
) : RemoteManagement {
  private var access: String? = null
  private val deleted: MutableSet<String> = mutableSetOf()

  override suspend fun init() {
    val localProfiles = secrets.listProfiles()
    val remoteProfiles = remoteMetadata.profiles.keys
    val localAndRemote = localProfiles intersect remoteProfiles
    val mismatchingHashes =
      localAndRemote.filter { SecretsState.metadata.profiles[it]!!.hash != remoteMetadata.profiles[it]!!.hash }
    val updated =
      if (mismatchingHashes.isEmpty()) listOf()
      else {
        println("These profiles exist locally but are not the same ones as on the remote.")
        prompts.removeFirstOrNull()?.split("\u0000")
          ?: KInquirer.promptCheckbox(
            "Select the local profiles you want to update from the remote, the rest will be pushed to the remote.",
            mismatchingHashes,
          )
      }
    val localToBeUpdated = (remoteProfiles subtract localProfiles) union updated
    localToBeUpdated.forEach {
      val key = superAccess.keys[it]!!
      val profileBytes = SecretsState.remote.getProfile(it)!!
      profileBytes.decrypt(key, encryption).deserialize<SecretProfile>(cbor)
      files.writeProfileBytes(it, profileBytes)
      files.writeKey(key)
      SecretsState.updateMetadata { add(remoteMetadata.profiles[it]!!) }
    }
    if (localToBeUpdated.isNotEmpty()) files.writeMetadata()
    superAccess = superAccess.copy(keys = superAccess.keys + localProfiles.associateWith { files.readKey(it) })
  }

  override fun listAccesses(): Set<String> = superAccess.access.keys

  override fun createAccess(name: String) {
    val mode = Prompts.accessMode(prompts)
    superAccess =
      superAccess.addAccess(
        name,
        when (mode) {
          EncryptionMode.PASSPHRASE ->
            UserKey(encryption.hashKeySHA256(Prompts.passphrase(prompts).encodeToByteString()))
          EncryptionMode.GPG -> UserKey(Prompts.gpgPublicKey())
        },
      )
  }

  override fun removeAccess(name: String) {
    superAccess = superAccess.removeAccess(name)
  }

  override fun selectAccess(name: String) {
    if (name !in superAccess.access) throw IllegalArgumentException(Values.accessDoesNotExistException(name))
    access = name
  }

  override fun addProfile(profile: String) {
    if (access == null) throw IllegalStateException(Values.NO_ACCESS_SELECTED_EXCEPTION)
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profile))
    access?.let { superAccess = superAccess.addToAccess(it, profile) }
  }

  override fun removeProfile(profile: String) {
    if (access == null) throw IllegalStateException(Values.NO_ACCESS_SELECTED_EXCEPTION)
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profile))
    if (profile !in superAccess.access[access!!]!!.second)
      throw IllegalArgumentException(Values.profileNotInAccess(profile))
    access?.let { superAccess = superAccess.removeFromAccess(it, profile) }
  }

  override fun listProfiles(): Set<String> {
    if (access == null) throw IllegalStateException(Values.NO_ACCESS_SELECTED_EXCEPTION)
    return superAccess.access[access!!]!!.second
  }

  override fun deselectAccess() {
    access = null
  }

  override fun deleteProfile(profile: String) {
    if (access != null) throw IllegalStateException("An access is selected.")
    if (profile !in SecretsState.metadata.profiles) throw IllegalArgumentException("Profile does not exist.")
    if (profile in deleted) throw IllegalArgumentException("Profile already deleted.")
    println("Deleting the profile will also delete it from your local secrets!")
    if (!(prompts.removeFirstOrNull()?.toBooleanStrict() ?: KInquirer.promptConfirm("Do you want to proceed?"))) return
    val accessesWithProfile = superAccess.access.filter { profile in it.value.second }.keys
    if (accessesWithProfile.isNotEmpty()) {
      println("The following access files contain the profile:")
      println(accessesWithProfile)
      println("If you proceed, the profile will be removed from these access files as well.")
      if (!(prompts.removeFirstOrNull()?.toBooleanStrict() ?: KInquirer.promptConfirm("Do you want to proceed?")))
        return
      superAccess =
        superAccess.copy(
          access =
            superAccess.access.mapValues {
              if (it.key !in accessesWithProfile) it.value else it.value.copy(second = it.value.second - profile)
            }
        )
    }
    superAccess = superAccess.copy(keys = superAccess.keys - profile)
    deleted.add(profile)
  }

  override fun restoreProfile(profile: String) {
    if (access != null) throw IllegalStateException("An access is selected.")
    if (profile !in deleted) throw IllegalStateException(Values.PROFILE_NOT_DELETED_EXCEPTION)
    deleted.remove(profile)
    superAccess = superAccess.addKey(files.readKey(profile))
  }

  override fun deletedProfiles(): Set<String> = deleted

  override suspend fun push() {
    val superBytes = superAccess.serialize(cbor).encrypt(superKey, encryption)
    val accesses =
      superAccess.access.mapValues {
        SecretAccess(it.key, it.value.second.associateWith { profile -> files.readKey(profile) })
          .serialize(cbor)
          .encrypt(it.value.first, encryption)
      }
    val newRemoteMetadata =
      remoteMetadata.copy(
        profiles = remoteMetadata.profiles + SecretsState.metadata.profiles - deleted,
        superAccess =
          AccessMetadata(Values.SUPER_ACCESS_METADATA_NAME, encryption.hashGitDataSHA1(superBytes), superKey),
        access =
          accesses
            .map { AccessMetadata(it.key, encryption.hashGitDataSHA1(it.value), superAccess.access[it.key]!!.first) }
            .associateBy { it.name },
      )
    SecretsState.remote.superUpdate(
      newRemoteMetadata.serialize(json),
      newRemoteMetadata.profiles
        .filter { it.key !in remoteMetadata.profiles || it.value.hash != remoteMetadata.profiles[it.key]!!.hash }
        .mapValues { files.profileBytes(it.key) },
      superBytes,
      accesses,
      deleted intersect remoteMetadata.profiles.keys,
    )
    deleted.forEach { files.removeProfile(it) }
    SecretsState.updateMetadata { removeAll(deleted) }
    files.writeMetadata()
  }
}
