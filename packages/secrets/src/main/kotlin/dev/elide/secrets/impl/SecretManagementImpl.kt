package dev.elide.secrets.impl

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.*
import com.github.kinquirer.core.Choice
import dev.elide.secrets.*
import dev.elide.secrets.Utils.choices
import dev.elide.secrets.Utils.decrypt
import dev.elide.secrets.Utils.deserialize
import dev.elide.secrets.Utils.encrypt
import dev.elide.secrets.Utils.hashKey
import dev.elide.secrets.Utils.serialize
import dev.elide.secrets.dto.persisted.*
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.remote.RemoteInitializer
import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import elide.annotations.Singleton
import elide.tooling.project.manifest.ElidePackageManifest

/** @author Lauri Heino <datafox> */
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

  override suspend fun init(path: Path, manifest: ElidePackageManifest?) {
    SecretsState.init(true, Utils.path(path))
    SystemFileSystem.createDirectories(SecretsState.path)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE ->
            UserKey(
              (Utils.passphrase() ?: Prompts.validateLocalPassphrase { files.canDecryptLocal(it) }).hashKey(encryption)
            )
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
    } else createData(manifest)
  }

  override fun listProfiles(): Set<String> = secrets.listProfiles()

  override fun loadProfile(profile: String) = secrets.loadProfile(profile)

  override fun getProfile(): String? = secrets.getProfile()

  override fun unloadProfile() = secrets.unloadProfile()

  override fun getEnv(): Map<String, String> = secrets.getEnv()

  override fun getSecret(name: String): Any? = secrets.getSecret(name)

  override fun getStringSecret(name: String): String? = secrets.getStringSecret(name)

  override fun getBinarySecret(name: String): ByteString? = secrets.getBinarySecret(name)

  override fun listSecrets(): Map<String, KClass<*>> = secrets.listSecrets()

  @OptIn(ExperimentalStdlibApi::class)
  override fun createProfile(profile: String) {
    if (profile in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileAlreadyExistsException(profile))
    val key = SecretKey(profile, Utils.generateBytes(Values.KEY_SIZE))
    val profile = SecretProfile(profile)
    val (profileBytes, keyBytes) = files.writeProfile(profile, key)
    SecretsState.updateMetadata {
      add(
        ProfileMetadata(
          profile.name,
          encryption.hashGitDataSHA1(profileBytes),
          encryption.hashGitDataSHA1(keyBytes),
        )
      )
    }
    files.writeMetadata()
  }

  override fun removeProfile(profile: String) {
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profile))
    if (SecretsState.profileFlow.value == null) secrets.loadProfile(profile)
    else if (SecretsState.profile.name != profile)
      throw IllegalStateException(Values.REMOVED_PROFILE_NOT_SELECTED_EXCEPTION)
    files.removeProfile(profile)
    SecretsState.updateMetadata { remove(profile) }
    files.writeMetadata()
  }

  override fun setStringSecret(name: String, value: String, envVar: String?) =
    setSecret(StringSecret(name, value, envVar))

  override fun setBinarySecret(name: String, value: ByteString) = setSecret(BinarySecret(name, value))

  override fun removeSecret(name: String) = SecretsState.updateProfile { remove(name) }

  override fun writeChanges() {
    files.writeProfile(SecretsState.profile, SecretsState.key)
  }

  override suspend fun pushToRemote() {
    if (SecretsState.profileFlow.value != null) throw IllegalStateException(Values.PROFILE_LOADED_PUSH_EXCEPTION)
    if (SecretsState.remoteFlow.value == null) initRemote()
    val accessName: String =
      SecretsState.local[Values.REMOTE_ACCESS_SECRET]
        ?: throw IllegalArgumentException(Values.REMOTE_MANAGEMENT_ONLY_EXCEPTION)
    val remoteMetadata: RemoteMetadata = SecretsState.remote.getMetadata()!!.deserialize(json)
    val accessMetadata = remoteMetadata.access[accessName]!!
    val (access, _) = getAccess(accessMetadata)
    val localProfiles = SecretsState.metadata.profiles
    val remoteProfiles = remoteMetadata.profiles.filterKeys { it in access.keys }
    val changed = localProfiles.filterValues { it.name in remoteProfiles && it.hash != remoteProfiles[it.name]!!.hash }
    val changedPushed =
      KInquirer.promptCheckboxObject(
          Values.PUSH_PROFILES_PROMPT,
          changed.choices(),
        )
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
      SecretsState.local[Values.REMOTE_ACCESS_SECRET]
        ?: throw IllegalArgumentException(Values.REMOTE_MANAGEMENT_ONLY_EXCEPTION)
    val remoteMetadata: RemoteMetadata = SecretsState.remote.getMetadata()!!.deserialize(json)
    val accessMetadata = remoteMetadata.access[accessName]!!
    val (access, _) = getAccess(accessMetadata)
    val localProfiles = SecretsState.metadata.profiles
    val remoteProfiles = remoteMetadata.profiles.filterKeys { it in access.keys }
    val changed =
      remoteProfiles.filterValues {
        it.name !in localProfiles ||
          it.hash != localProfiles[it.name]!!.hash ||
          it.keyHash != localProfiles[it.name]!!.keyHash
      }
    val newProfiles =
      changed.map {
        val key = access.keys[it.key]!!
        SecretsState.remote.getProfile(it.key)!!.decrypt(key, encryption).deserialize<SecretProfile>(cbor) to key
      }
    SecretsState.updateMetadata { copy(profiles = profiles + changed) }
    newProfiles.forEach { files.writeProfile(it.first, it.second) }
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
              ?: KInquirer.promptInputPassword(Values.SUPERUSER_PASSPHRASE_PROMPT).hashKey(encryption)
          )
        EncryptionMode.GPG -> UserKey(remoteMetadata.superAccess.fingerprint!!)
      }
    val superAccess: SuperAccess =
      SecretsState.remote.getSuperAccess()!!.decrypt(superKey, encryption).deserialize(cbor)
    return RemoteManagementImpl(secrets, files, encryption, json, cbor, remoteMetadata, superAccess, superKey).apply {
      init()
      SecretsState.updateLocal { add(BinarySecret(Values.SUPER_ACCESS_KEY_SECRET, superKey.key)) }
      files.writeLocal()
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun createData(manifest: ElidePackageManifest?) {
    println(Values.WELCOME_MESSAGE)
    val mode = Prompts.localUserKeyMode()
    SecretsState.userKey =
      when (mode) {
        EncryptionMode.PASSPHRASE -> UserKey((Utils.passphrase() ?: Prompts.passphrase()).hashKey(encryption))
        EncryptionMode.GPG -> UserKey(Prompts.gpgPrivateKey())
      }
    SecretsState.local = LocalProfile()
    println(Values.INIT_OR_PULL_MESSAGE)
    if (
      KInquirer.promptListObject(
        Values.GENERIC_CHOICE_PROMPT,
        listOf(
          Choice(Values.INITIALIZE_PROJECT_OPTION, true),
          Choice(Values.PULL_PROJECT_OPTION, false),
        ),
      )
    )
      initializeData(manifest)
    else importData()
    files.writeLocal()
    files.writeMetadata()
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun initializeData(manifest: ElidePackageManifest?) {
    val name = KInquirer.promptInput(Values.PROJECT_NAME_PROMPT, manifest?.name ?: "")
    SecretsState.metadata = LocalMetadata(name, SecretsState.userKey)
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun importData() {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val remoteMetadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalStateException(Values.REMOTE_NOT_INITIALIZED_EXCEPTION)
    SecretsState.metadata = LocalMetadata(remoteMetadata.name, SecretsState.userKey)
    if (KInquirer.promptConfirm(Values.PULL_AS_SUPERUSER_PROMPT)) {
      manageRemote()
      return
    }
    val (access, key) = selectAccess()
    access.keys.forEach { (name, key) ->
      val profileBytes = SecretsState.remote.getProfile(name)!!
      val profile: SecretProfile = profileBytes.decrypt(key, encryption).deserialize(cbor)
      val (_, keyBytes) = files.writeProfile(profile, key)
      SecretsState.updateMetadata {
        add(
          ProfileMetadata(
            name,
            encryption.hashGitDataSHA1(profileBytes),
            encryption.hashGitDataSHA1(keyBytes),
          )
        )
      }
    }
    SecretsState.updateLocal {
      add(StringSecret(Values.REMOTE_ACCESS_SECRET, access.name))
        .add(BinarySecret(Values.REMOTE_ACCESS_KEY_SECRET, key.key))
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
      KInquirer.promptListObject(
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
            val pass = KInquirer.promptInputPassword(Values.ACCESS_PASSPHRASE_PROMPT)
            UserKey(pass.hashKey(encryption))
          }
          EncryptionMode.GPG -> UserKey(access.fingerprint!!)
        }
    return accessBytes.decrypt(key, encryption).deserialize<SecretAccess>(cbor) to key
  }

  private suspend fun getAccess(name: String): Pair<SecretAccess, UserKey> {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val metadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalArgumentException(Values.REMOTE_NOT_INITIALIZED_EXCEPTION)
    return getAccess(metadata.access[name]!!)
  }

  private suspend fun initRemote() {
    val init = KInquirer.promptListObject(Values.REMOTE_SECRETS_LOCATION_PROMPT, remoteInitializers.choices())
    SecretsState.remote = init.initialize()
    files.writeLocal()
  }

  private fun <T> setSecret(secret: Secret<T>) = SecretsState.updateProfile { add(secret) }

  private suspend fun createRemote(): RemoteMetadata {
    val mode = Prompts.superKeyMode()
    val superKey: UserKey =
      when (mode) {
        EncryptionMode.PASSPHRASE -> UserKey(Prompts.passphrase().hashKey(encryption))
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
    SecretsState.remote.superUpdate(remoteMetadata.serialize(json), mapOf(), superBytes, mapOf())
    SecretsState.updateLocal { add(BinarySecret(Values.SUPER_ACCESS_KEY_SECRET, superKey.key)) }
    files.writeLocal()
    return remoteMetadata
  }
}
