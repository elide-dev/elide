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

import elide.secrets.*
import elide.secrets.dto.persisted.EncryptionMode
import elide.secrets.dto.persisted.Profile.Companion.get
import elide.secrets.dto.persisted.UserKey
import elide.annotations.Singleton
import elide.tooling.project.manifest.ElidePackageManifest
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import elide.runtime.Logger
import elide.runtime.Logging
import elide.secrets.Utils.hashKey

/** @author Lauri Heino <datafox> */
@Singleton
internal class SecretsImpl(private val encryption: Encryption, private val files: FileManagement) : Secrets {
  override val initialized: Boolean
    get() = SecretsState.initialized
  private val logger: Logger = Logging.of(SecretsImpl::class)
  private var passphraseOverride: String? = null

  override suspend fun init(path: Path, manifest: ElidePackageManifest?) {
    SecretsState.init(false, Utils.path(path), manifest)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE ->
            UserKey(
              (passphraseOverride ?: Utils.passphrase())?.hashKey(encryption)
                ?: throw IllegalStateException(Values.PASSPHRASE_READ_EXCEPTION)
            )
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
      manifest?.secrets?.profile?.let { loadProfile(it) }
    } else logger.warn(Values.SECRETS_NOT_INITIALIZED_WARNING)
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

  override fun getEnv(): Map<String, String> = SecretsState.profileFlow.value?.first?.getEnv() ?: mapOf()

  override fun getSecret(name: String): Any? = SecretsState.profile.secrets[name]?.value

  override fun getStringSecret(name: String): String? = SecretsState.profile[name]

  override fun getBinarySecret(name: String): ByteString? = SecretsState.profile[name]

  override fun listSecrets(): Map<String, SecretType> = SecretsState.profile.listSecrets()

  internal fun overridePassphrase(passphrase: String?) {
    passphraseOverride = passphrase
  }
}
