package dev.elide.secrets.impl

import dev.elide.secrets.*
import dev.elide.secrets.dto.persisted.EncryptionMode
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.dto.persisted.UserKey
import io.micronaut.core.annotation.Order
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.files.Path
import kotlin.reflect.KClass
import elide.annotations.Singleton

/** @author Lauri Heino <datafox> */
@Singleton
@Order(-10)
internal sealed class SecretsImpl(private val encryption: Encryption, private val files: FileManagement) : Secrets {
  override suspend fun init(path: Path) {
    SecretsState.init(false, path)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE ->
            UserKey(
              encryption.hashKeySHA256(
                Utils.passphrase()?.encodeToByteString() ?: throw IllegalStateException("Could not read passphrase")
              )
            )
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
    } else throw IllegalStateException("Secrets are not initialized, run \"elide secrets\" first")
  }

  override fun listProfiles(): Set<String> = SecretsState.metadata.profiles.keys

  override fun loadProfile(profile: String) {
    if (profile !in SecretsState.metadata.profiles) throw IllegalArgumentException("Profile $profile does not exist")
    SecretsState.profilePair = files.readProfile(profile)
  }

  override fun unloadProfile() {
    SecretsState.profileFlow.value = null
  }

  override fun getEnv(): Map<String, String> = SecretsState.profile.getEnv()

  override fun getStringSecret(name: String): String? = SecretsState.profile[name]

  override fun getBinarySecret(name: String): ByteString? = SecretsState.profile[name]

  override fun listSecrets(): Map<String, KClass<*>> =
    SecretsState.profile.secrets.map { (key, value) -> key to value.value::class }.toMap()
}
