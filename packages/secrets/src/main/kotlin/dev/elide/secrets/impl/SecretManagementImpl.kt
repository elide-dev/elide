package dev.elide.secrets.impl

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.*
import com.github.kinquirer.core.Choice
import dev.elide.secrets.*
import dev.elide.secrets.Utils.decrypt
import dev.elide.secrets.Utils.deserialize
import dev.elide.secrets.Utils.encrypt
import dev.elide.secrets.Utils.serialize
import dev.elide.secrets.dto.persisted.*
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.remote.RemoteInitializer
import io.micronaut.core.annotation.Order
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.json.Json
import elide.annotations.Singleton

/** @author Lauri Heino <datafox> */
@Singleton
@Order(10)
internal class SecretManagementImpl(
  private val encryption: Encryption,
  private val files: FileManagement,
  private val json: Json,
  private val cbor: BinaryFormat,
  private val remoteInitializers: List<RemoteInitializer>,
) : SecretsImpl(encryption, files), SecretManagement {
  override suspend fun init(path: Path) {
    SecretsState.init(true, path)
    SystemFileSystem.createDirectories(SecretsState.path)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE ->
            UserKey(
              encryption.hashKeySHA256(
                (Utils.passphrase() ?: Prompts.validateLocalPassphrase { files.canDecryptLocal(it) })
                  .encodeToByteString()
              )
            )
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
    } else createData()
  }

  @OptIn(ExperimentalStdlibApi::class)
  override fun createProfile(profile: String) {
    if (profile in SecretsState.metadata.profiles) throw IllegalArgumentException("Profile $profile already exists")
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
    if (profile !in SecretsState.metadata.profiles) throw IllegalArgumentException("Profile $profile already exists")
    if (SecretsState.profileFlow.value == null) loadProfile(profile)
    else if (SecretsState.profile.name != profile)
      throw IllegalStateException("Can only remove currently selected profile")
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
    if (SecretsState.profileFlow.value != null) throw IllegalStateException("Please unload the profile before pushing")
    if (SecretsState.remoteFlow.value == null) initRemote()
    val accessName: String =
      SecretsState.local["remote:access"]
        ?: throw IllegalArgumentException(
          "Secrets were created locally or pulled as a superuser, please use remote management instead."
        )
    val remoteMetadata: RemoteMetadata = SecretsState.remote.getMetadata()!!.deserialize(json)
    val accessMetadata = remoteMetadata.access[accessName]!!
    val (access, _) = getAccess(accessMetadata)
    val localProfiles = SecretsState.metadata.profiles
    val remoteProfiles = remoteMetadata.profiles.filterKeys { it in access.keys }
    val changed = localProfiles.filterValues { it.name in remoteProfiles && it.hash != remoteProfiles[it.name]!!.hash }
    val changedPushed =
      KInquirer.promptCheckboxObject(
          "Please select the changed profiles you want to push:",
          changed.map { Choice(it.key, it) },
        )
        .associate { it.key to it.value }
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
    if (SecretsState.profileFlow.value != null) throw IllegalStateException("Please unload the profile before pulling")
    if (SecretsState.remoteFlow.value == null) initRemote()
    val accessName: String =
      SecretsState.local["remote:access"]
        ?: throw IllegalArgumentException(
          "Secrets were created locally or pulled as a superuser, please use remote management instead."
        )
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
            SecretsState.local["remote:super"]
              ?: encryption.hashKeySHA256(
                KInquirer.promptInputPassword("Please enter the superuser passphrase:").encodeToByteString()
              )
          )
        EncryptionMode.GPG -> UserKey(remoteMetadata.superAccess.fingerprint!!)
      }
    val superAccess: SuperAccess =
      SecretsState.remote.getSuperAccess()!!.decrypt(superKey, encryption).deserialize(cbor)
    return RemoteManagementImpl(this, files, encryption, json, cbor, remoteMetadata, superAccess, superKey).apply {
      init()
      SecretsState.updateLocal { add(BinarySecret("remote:super", superKey.key)) }
      files.writeLocal()
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun createData() {
    println("Welcome to Elide Secrets! Let's get you set up")
    val mode = Prompts.localUserKeyMode()
    SecretsState.userKey =
      when (mode) {
        EncryptionMode.PASSPHRASE ->
          UserKey(encryption.hashKeySHA256((Utils.passphrase() ?: Prompts.passphrase()).encodeToByteString()))
        EncryptionMode.GPG -> UserKey(Prompts.gpgPrivateKey())
      }
    SecretsState.local = LocalProfile()
    if (
      KInquirer.promptListObject(
        "Do you want to initialize a new project or pull an existing project? Please note that if you initialize a new project, you can only push it as a superuser.",
        listOf(
          Choice("Initialize a new project", true),
          Choice("Pull an existing project", false),
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
    val name = KInquirer.promptInput("What is the name of your project?")
    val organization = KInquirer.promptInput("What is the name of your organization?")
    SecretsState.metadata = LocalMetadata(name, organization, SecretsState.userKey)
  }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun importData() {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val remoteMetadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalStateException("Remote is not initialized, please run remote management")
    SecretsState.metadata = LocalMetadata(remoteMetadata.name, remoteMetadata.organization, SecretsState.userKey)
    if (KInquirer.promptConfirm("Do you want to pull as a superuser?")) {
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
      add(StringSecret("remote:access", access.name)).add(BinarySecret("remote:access:${access.name}", key.key))
    }
    files.writeLocal()
    files.writeMetadata()
  }

  private suspend fun selectAccess(): Pair<SecretAccess, UserKey> {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val metadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalArgumentException("Remote has not been initialized")
    val fingerprints = GPGHandler.gpgPrivateKeys().values.map { it.lowercase() }.toSet()
    val accesses =
      metadata.access.values.filter {
        it.mode == EncryptionMode.PASSPHRASE || it.fingerprint!!.lowercase() in fingerprints
      }
    val access =
      KInquirer.promptListObject(
        "Please select the access file you want to import. Please note that for GPG-encrypted access files, only the ones that you have a private key for on this system will be displayed!",
        accesses.map { Choice(it.name + if (it.fingerprint != null) " (${it.fingerprint})" else "", it) },
      )
    return getAccess(access)
  }

  private suspend fun getAccess(access: AccessMetadata): Pair<SecretAccess, UserKey> {
    val accessBytes = SecretsState.remote.getAccess(access.name)!!
    val key =
      SecretsState.local.get<ByteString>("remote:access:${access.name}")?.let { UserKey(access.mode, it) }
        ?: when (access.mode) {
          EncryptionMode.PASSPHRASE -> {
            val pass = KInquirer.promptInputPassword("Please enter the passphrase for this access file:")
            UserKey(encryption.hashKeySHA256(pass.encodeToByteString()))
          }
          EncryptionMode.GPG -> UserKey(access.fingerprint!!)
        }
    return accessBytes.decrypt(key, encryption).deserialize<SecretAccess>(cbor) to key
  }

  private suspend fun getAccess(name: String): Pair<SecretAccess, UserKey> {
    if (SecretsState.remoteFlow.value == null) initRemote()
    val metadata: RemoteMetadata =
      SecretsState.remote.getMetadata()?.deserialize(json)
        ?: throw IllegalArgumentException("Remote has not been initialized")
    return getAccess(metadata.access[name]!!)
  }

  private suspend fun initRemote() {
    val init =
      KInquirer.promptListObject("Please select where to access remote secrets:", remoteInitializers.map { Choice(it.id, it) })
    SecretsState.remote = init.initialize()
    files.writeLocal()
  }

  private fun <T> setSecret(secret: Secret<T>) = SecretsState.updateProfile { add(secret) }

  private suspend fun createRemote(): RemoteMetadata {
    val mode = Prompts.superKeyMode()
    val superKey: UserKey =
      when (mode) {
        EncryptionMode.PASSPHRASE -> UserKey(encryption.hashKeySHA256(Prompts.passphrase().encodeToByteString()))
        EncryptionMode.GPG -> UserKey(Prompts.gpgPrivateKey())
      }
    val superAccess = SuperAccess(mapOf(), mapOf())
    val superBytes = superAccess.serialize(cbor).encrypt(superKey, encryption)
    val superAccessMetadata = AccessMetadata("super", encryption.hashGitDataSHA1(superBytes), superKey)
    val remoteMetadata =
      RemoteMetadata(
        SecretsState.metadata.name,
        SecretsState.metadata.organization,
        mapOf(),
        superAccessMetadata,
        mapOf(),
      )
    SecretsState.remote.superUpdate(remoteMetadata.serialize(json), mapOf(), superBytes, mapOf())
    SecretsState.updateLocal { add(BinarySecret("remote:super", superKey.key)) }
    files.writeLocal()
    return remoteMetadata
  }
}
