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
import com.github.kinquirer.components.*
import com.github.kinquirer.core.Choice
import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.json.Json
import elide.annotations.Singleton
import elide.secrets.*
import elide.secrets.Utils.choices
import elide.secrets.Utils.decrypt
import elide.secrets.Utils.deserialize
import elide.secrets.Utils.encrypt
import elide.secrets.Utils.hashKey
import elide.secrets.Utils.serialize
import elide.secrets.dto.persisted.*
import elide.secrets.dto.persisted.Profile.Companion.get
import elide.secrets.remote.RemoteInitializer
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Implementation of [SecretManagement].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class SecretManagementImpl(
  private val secrets: Secrets,
  private val encryption: Encryption,
  private val files: FileManagement,
  private val json: Json,
  private val cbor: BinaryFormat,
  private val remoteInitializers: List<RemoteInitializer>,
) : SecretManagement {
  override val initialized: Boolean
    get() = secrets.initialized

  private var localCopy: LocalProfile? = null
  private val prompts: MutableList<String> = mutableListOf()

  override suspend fun init(path: Path, manifest: ElidePackageManifest?) {
    SecretsState.init(true, Utils.path(path), manifest)
    SystemFileSystem.createDirectories(SecretsState.path)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE -> {
            val pass =
              System.getenv(Values.PASSPHRASE_ENVIRONMENT_VARIABLE)
                ?: Prompts.validateLocalPassphrase(prompts) { files.canDecryptLocal(it) }
            UserKey(pass.hashKey(encryption))
          }
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
    } else createData()
  }

  override suspend fun initNonInteractive(path: Path, manifest: ElidePackageManifest) {
    SecretsState.init(false, Utils.path(path), manifest)
    SystemFileSystem.createDirectories(SecretsState.path)
    val pass =
      prompts.removeFirstOrNull()
        ?: System.getenv(Values.PASSPHRASE_ENVIRONMENT_VARIABLE)
        ?: throw IllegalStateException(Values.PASSPHRASE_READ_EXCEPTION)
    val remoteType = manifest.secrets?.remote ?: throw IllegalStateException(Values.REMOTE_NOT_SPECIFIED_EXCEPTION)
    val accessName =
      prompts.removeFirstOrNull()
        ?: System.getenv(Values.ACCESS_NAME_ENVIRONMENT_VARIABLE)
        ?: throw IllegalStateException(Values.ACCESS_NAME_READ_EXCEPTION)
    val accessPass =
      prompts.removeFirstOrNull()
        ?: System.getenv(Values.ACCESS_PASSPHRASE_ENVIRONMENT_VARIABLE)
        ?: throw IllegalStateException(Values.ACCESS_PASSPHRASE_READ_EXCEPTION)
    val profileName =
      System.getenv(Values.PROFILE_OVERRIDE_ENVIRONMENT_VARIABLE)
        ?: manifest.secrets?.profile
        ?: throw IllegalStateException(Values.PROFILE_NOT_SPECIFIED_EXCEPTION)
    SecretsState.userKey = UserKey(pass.hashKey(encryption))
    SecretsState.local = LocalProfile()
    val remoteInit = remoteInitializers.find { it.name == remoteType.symbol }!!
    val remote = remoteInit.initNonInteractive()
    val remoteMetadata: RemoteMetadata =
      remote.getMetadata()?.deserialize(json) ?: throw IllegalStateException(Values.REMOTE_NOT_INITIALIZED_EXCEPTION)
    if (accessName !in remoteMetadata.access)
      throw IllegalArgumentException(Values.accessDoesNotExistException(accessName))
    val accessMetadata = remoteMetadata.access[accessName]!!
    if (accessMetadata.mode != EncryptionMode.PASSPHRASE)
      throw IllegalStateException(Values.NON_INTERACTIVE_ACCESS_MUST_USE_PASSPHRASE_EXCEPTION)
    if (profileName !in remoteMetadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profileName))
    val access: SecretAccess =
      remote.getAccess(accessName)!!.decrypt(UserKey(accessPass.hashKey(encryption)), encryption).deserialize(cbor)
    val profileBytes = remote.getProfile(profileName)!!
    val profileKey = access.keys[profileName]!!
    profileBytes.decrypt(profileKey, encryption).deserialize<SecretProfile>(cbor)
    SecretsState.metadata =
      LocalMetadata(remoteMetadata.name, SecretsState.userKey, remoteMetadata.profiles[profileName]!!)
    files.writeProfileBytes(profileName, profileBytes)
    files.writeKey(profileKey)
    files.writeLocal()
    files.writeMetadata()
  }

  override fun listProfiles(): Set<String> = secrets.listProfiles()

  override fun loadProfile(profile: String) = secrets.loadProfile(profile)

  override fun getProfile(): String? = secrets.getProfile()

  override fun unloadProfile() {
    if (localCopy != null) localCopy = null else secrets.unloadProfile()
  }

  override fun getEnv(): Map<String, String> = if (localCopy != null) mapOf() else secrets.getEnv()

  override fun getSecret(name: String): Any? = localCopy?.let { it[name]!! } ?: secrets.getSecret(name)

  override fun getStringSecret(name: String): String? = localCopy?.let { it[name]!! } ?: secrets.getStringSecret(name)

  override fun getBinarySecret(name: String): ByteString? =
    localCopy?.let { it[name]!! } ?: secrets.getBinarySecret(name)

  override fun listSecrets(): Map<String, SecretType> = localCopy?.listSecrets() ?: secrets.listSecrets()

  override fun loadLocalProfile() {
    localCopy = SecretsState.local
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun createProfile(profile: String) {
    if (profile in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileAlreadyExistsException(profile))
    val key = SecretKey(profile, Utils.generateBytes(Values.KEY_SIZE))
    val profile = SecretProfile(profile)
    val profileBytes = files.writeProfile(profile, key)
    SecretsState.updateMetadata { add(ProfileMetadata(profile.name, encryption.hashGitDataSHA1(profileBytes))) }
    files.writeMetadata()
  }

  override fun deleteProfile(profile: String) {
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profile))
    if (SecretsState.profileFlow.value == null) secrets.loadProfile(profile)
    else if (SecretsState.profile.name != profile)
      throw IllegalStateException(Values.REMOVED_PROFILE_NOT_SELECTED_EXCEPTION)
    files.removeProfile(profile)
    SecretsState.updateMetadata { remove(profile) }
    files.writeMetadata()
    unloadProfile()
  }

  override fun setTextSecret(name: String, value: String, envVar: String?) =
    StringSecret(name, value, envVar).run { localCopy?.let { localCopy = it.add(this) } ?: setSecret(this) }

  override fun updateTextSecret(name: String, value: String) {
    setTextSecret(name, value, (localCopy ?: SecretsState.profile).get<StringSecret>(name)?.env)
  }

  override fun setBinarySecret(name: String, value: ByteString) =
    BinarySecret(name, value).run { localCopy?.let { localCopy = it.add(this) } ?: setSecret(this) }

  override fun removeSecret(name: String) =
    localCopy?.let { localCopy = it.remove(name) } ?: SecretsState.updateProfile { remove(name) }

  override fun writeChanges() {
    localCopy?.let {
      SecretsState.local = it
      files.writeLocal()
      return
    }
    val profileBytes = files.writeProfile(SecretsState.profile, SecretsState.key)
    SecretsState.updateMetadata {
      add(ProfileMetadata(SecretsState.profile.name, encryption.hashGitDataSHA1(profileBytes)))
    }
    files.writeMetadata()
  }

  override suspend fun pushToRemote() {
    if (SecretsState.profileFlow.value != null) throw IllegalStateException(Values.PROFILE_LOADED_PUSH_EXCEPTION)
    if (SecretsState.remoteFlow.value == null) initRemote()
    val accessName: String =
      SecretsState.local[Values.SELECTED_REMOTE_ACCESS_SECRET]
        ?: throw IllegalArgumentException(Values.REMOTE_MANAGEMENT_ONLY_EXCEPTION)
    val remoteMetadata: RemoteMetadata = SecretsState.remote.getMetadata()!!.deserialize(json)
    val accessMetadata = remoteMetadata.access[accessName]!!
    val (access, _) = getAccess(accessMetadata)
    val localProfiles = SecretsState.metadata.profiles
    val remoteProfiles = remoteMetadata.profiles.filterKeys { it in access.keys }
    val changed = localProfiles.filterValues { it.name in remoteProfiles && it.hash != remoteProfiles[it.name]!!.hash }
    if (changed.isEmpty()) {
      println(Values.NO_CHANGED_PROFILES_MESSAGE)
      return
    }
    val changedPushed =
      (prompts.removeFirstOrNull()?.let { it.split("\u0000").map { profile -> changed[profile]!! } }
          ?: KInquirer.promptCheckboxObject(
            Values.PUSH_PROFILES_PROMPT,
            changed.choices(),
          ))
        .associateBy { it.name }
    val newMetadata =
      remoteMetadata.copy(
        profiles =
          remoteMetadata.profiles.mapValues {
            if (it.key in changedPushed) it.value.copy(hash = changedPushed[it.key]!!.hash) else it.value
          }
      )
    SecretsState.remote.update(newMetadata.serialize(json), changedPushed.mapValues { files.profileBytes(it.key) })
  }

  override suspend fun pullFromRemote() {
    if (SecretsState.profileFlow.value != null) throw IllegalStateException(Values.PROFILE_LOADED_PULL_EXCEPTION)
    if (SecretsState.remoteFlow.value == null) initRemote()
    val accessName: String =
      SecretsState.local[Values.SELECTED_REMOTE_ACCESS_SECRET]
        ?: throw IllegalArgumentException(Values.REMOTE_MANAGEMENT_ONLY_EXCEPTION)
    val remoteMetadata: RemoteMetadata = SecretsState.remote.getMetadata()!!.deserialize(json)
    val accessMetadata = remoteMetadata.access[accessName]!!
    val (access, _) = getAccess(accessMetadata)
    val localProfiles = SecretsState.metadata.profiles
    val remoteProfiles = remoteMetadata.profiles.filterKeys { it in access.keys }
    val changed = remoteProfiles.filterValues { it.name !in localProfiles || it.hash != localProfiles[it.name]!!.hash }
    val newProfiles =
      changed.map {
        val key = access.keys[it.key]!!
        val profileBytes = SecretsState.remote.getProfile(it.key)!!
        profileBytes.decrypt(key, encryption).deserialize<SecretProfile>(cbor)
        profileBytes to key
      }
    SecretsState.updateMetadata { copy(profiles = profiles + changed) }
    newProfiles.forEach {
      files.writeProfileBytes(it.second.name, it.first)
      files.writeKey(it.second)
    }
    files.writeMetadata()
  }

  override suspend fun manageRemote(): RemoteManagement {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val remoteMetadataBytes = SecretsState.remote.getMetadata()
    val remoteMetadata = remoteMetadataBytes?.deserialize(json) ?: createRemote()
    val superKey: UserKey =
      when (remoteMetadata.superAccess.mode) {
        EncryptionMode.PASSPHRASE ->
          UserKey(
            SecretsState.local[Values.SUPER_ACCESS_KEY_SECRET]
              ?: prompts.removeFirstOrNull()?.hashKey(encryption)
              ?: KInquirer.promptInputPassword(Values.SUPERUSER_PASSPHRASE_PROMPT).hashKey(encryption)
          )
        EncryptionMode.GPG -> UserKey(remoteMetadata.superAccess.fingerprint!!)
      }
    val superAccess: SuperAccess =
      SecretsState.remote.getSuperAccess()!!.decrypt(superKey, encryption).deserialize(cbor)
    return RemoteManagementImpl(secrets, files, encryption, json, cbor, remoteMetadata, superAccess, superKey, prompts)
      .apply {
        init()
        SecretsState.updateLocal { add(BinarySecret(Values.SUPER_ACCESS_KEY_SECRET, superKey.key)) }
        files.writeLocal()
      }
  }

  internal fun queuePrompt(prompt: String) = prompts.add(prompt)

  internal fun <E : Enum<E>> queuePrompt(enum: Enum<E>) = prompts.add(enum.name)

  internal fun queuePrompt(bool: Boolean) = prompts.add(bool.toString())

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun createData() {
    println(Values.WELCOME_MESSAGE)
    val mode = Prompts.localUserKeyMode(prompts)
    SecretsState.userKey =
      when (mode) {
        EncryptionMode.PASSPHRASE ->
          UserKey(
            (System.getenv(Values.PASSPHRASE_ENVIRONMENT_VARIABLE) ?: Prompts.passphrase(prompts)).hashKey(encryption)
          )
        EncryptionMode.GPG -> UserKey(Prompts.gpgPrivateKey())
      }
    SecretsState.local = LocalProfile()
    println(Values.INIT_OR_PULL_MESSAGE)
    if (
      prompts.removeFirstOrNull()?.toBooleanStrict()
        ?: KInquirer.promptListObject(
          Values.GENERIC_CHOICE_PROMPT,
          listOf(
            Choice(Values.INITIALIZE_PROJECT_OPTION, true),
            Choice(Values.PULL_PROJECT_OPTION, false),
          ),
        )
    )
      initializeData()
    else importData()
    files.writeLocal()
    files.writeMetadata()
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun initializeData() {
    val name =
      prompts.removeFirstOrNull()
        ?: KInquirer.promptInput(Values.PROJECT_NAME_PROMPT, SecretsState.manifest?.name ?: "")
    SecretsState.metadata = LocalMetadata(name, SecretsState.userKey)
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun importData() {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val remoteMetadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalStateException(Values.REMOTE_NOT_INITIALIZED_EXCEPTION)
    SecretsState.metadata = LocalMetadata(remoteMetadata.name, SecretsState.userKey)
    if (prompts.removeFirstOrNull()?.toBooleanStrict() ?: KInquirer.promptConfirm(Values.PULL_AS_SUPERUSER_PROMPT)) {
      manageRemote()
      return
    }
    val (access, key) = selectAccess()
    access.keys.forEach { (name, key) ->
      val profileBytes = SecretsState.remote.getProfile(name)!!
      profileBytes.decrypt(key, encryption).deserialize<SecretProfile>(cbor)
      files.writeProfileBytes(name, profileBytes)
      files.writeKey(key)
      SecretsState.updateMetadata {
        add(
          ProfileMetadata(
            name,
            encryption.hashGitDataSHA1(profileBytes),
          )
        )
      }
    }
    SecretsState.updateLocal {
      addAll(
        StringSecret(Values.SELECTED_REMOTE_ACCESS_SECRET, access.name),
        BinarySecret(Values.REMOTE_ACCESS_KEY_SECRET, key.key),
      )
    }
    files.writeLocal()
    files.writeMetadata()
  }

  private suspend fun selectAccess(): Pair<SecretAccess, UserKey> {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val metadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalArgumentException(Values.REMOTE_NOT_INITIALIZED_EXCEPTION)
    val fingerprints = GPGHandler.gpgPrivateKeys().values.map { it.lowercase() }.toSet()
    val accesses =
      metadata.access.values.filter {
        it.mode == EncryptionMode.PASSPHRASE || it.fingerprint!!.lowercase() in fingerprints
      }
    println(Values.SELECT_ACCESS_IMPORT_MESSAGE)
    val access =
      prompts.removeFirstOrNull()?.let { accesses.find { access -> access.name == it }!! }
        ?: KInquirer.promptListObject(
          Values.GENERIC_CHOICE_PROMPT,
          accesses.choices { name + if (fingerprint != null) " ($fingerprint)" else "" },
        )
    return getAccess(access)
  }

  private suspend fun getAccess(access: AccessMetadata): Pair<SecretAccess, UserKey> {
    val accessBytes = SecretsState.remote.getAccess(access.name)!!
    val key =
      SecretsState.local.get<ByteString>(Values.REMOTE_ACCESS_KEY_SECRET)?.let { UserKey(access.mode, it) }
        ?: when (access.mode) {
          EncryptionMode.PASSPHRASE -> {
            val pass = prompts.removeFirstOrNull() ?: KInquirer.promptInputPassword(Values.ACCESS_PASSPHRASE_PROMPT)
            UserKey(pass.hashKey(encryption))
          }
          EncryptionMode.GPG -> UserKey(access.fingerprint!!)
        }
    return accessBytes.decrypt(key, encryption).deserialize<SecretAccess>(cbor) to key
  }

  private suspend fun initRemote() {
    val init =
      (SecretsState.local.get<String>(Values.REMOTE_SECRET) ?: prompts.removeFirstOrNull())?.let { name ->
        remoteInitializers.find { name == it.name }
      } ?: KInquirer.promptListObject(Values.REMOTE_SECRETS_LOCATION_PROMPT, remoteInitializers.choices())
    SecretsState.remote = init.init(prompts)
    SecretsState.updateLocal { add(StringSecret(Values.REMOTE_SECRET, init.name)) }
    files.writeLocal()
  }

  private fun <T> setSecret(secret: Secret<T>) = SecretsState.updateProfile { add(secret) }

  private suspend fun createRemote(): RemoteMetadata {
    val mode = Prompts.superKeyMode(prompts)
    val superKey: UserKey =
      when (mode) {
        EncryptionMode.PASSPHRASE -> UserKey(Prompts.passphrase(prompts).hashKey(encryption))
        EncryptionMode.GPG -> UserKey(Prompts.gpgPrivateKey())
      }
    val superAccess = SuperAccess(mapOf(), mapOf())
    val superBytes = superAccess.serialize(cbor).encrypt(superKey, encryption)
    val superAccessMetadata =
      AccessMetadata(Values.SUPER_ACCESS_METADATA_NAME, encryption.hashGitDataSHA1(superBytes), superKey)
    val remoteMetadata =
      RemoteMetadata(
        SecretsState.metadata.name,
        mapOf(),
        superAccessMetadata,
        mapOf(),
      )
    SecretsState.remote.superUpdate(remoteMetadata.serialize(json), mapOf(), superBytes, mapOf(), setOf(), setOf())
    SecretsState.updateLocal { add(BinarySecret(Values.SUPER_ACCESS_KEY_SECRET, superKey.key)) }
    files.writeLocal()
    return remoteMetadata
  }
}
