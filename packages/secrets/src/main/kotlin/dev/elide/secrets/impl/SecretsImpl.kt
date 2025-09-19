package dev.elide.secrets.impl

import dev.elide.secrets.*
import dev.elide.secrets.dto.persisted.EncryptionMode
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.dto.persisted.UserKey
import elide.annotations.Singleton
import elide.tooling.project.manifest.ElidePackageManifest
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

/** @author Lauri Heino <datafox> */
@Singleton
internal class SecretsImpl(private val encryption: Encryption, private val files: FileManagement) : Secrets {
  override val initialized: Boolean
    get() = SecretsState.initialized

  override suspend fun init(path: Path, manifest: ElidePackageManifest?) {
    SecretsState.init(false, Utils.path(path))
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE ->
            UserKey(
              encryption.hashKeySHA256(
                Utils.passphrase()?.encodeToByteString()
                  ?: throw IllegalStateException(Values.PASSPHRASE_READ_EXCEPTION)
              )
            )
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
    } else throw IllegalStateException(Values.SECRETS_NOT_INITIALIZED_EXCEPTION)
    manifest?.secrets?.profile?.let { loadProfile(it) }
  }

  override fun listProfiles(): Set<String> = SecretsState.metadata.profiles.keys

  override fun loadProfile(profile: String) {
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(Values.profileDoesNotExistException(profile))
    SecretsState.profilePair = files.readProfile(profile)
  }

  override fun getProfile(): String? = SecretsState.profileFlow.value?.first?.name

  override fun unloadProfile() {
    SecretsState.profileFlow.value = null
  }

  override fun getEnv(): Map<String, String> = SecretsState.profile.getEnv()

  override fun getSecret(name: String): Any? = SecretsState.profile.secrets[name]?.value

  override fun getStringSecret(name: String): String? = SecretsState.profile[name]

  override fun getBinarySecret(name: String): ByteString? = SecretsState.profile[name]

  override fun listSecrets(): Map<String, KClass<*>> =
    SecretsState.profile.secrets.map { (key, value) -> key to value.value::class }.toMap()
}
