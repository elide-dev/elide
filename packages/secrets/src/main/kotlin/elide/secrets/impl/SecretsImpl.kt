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

import java.nio.file.Path
import kotlinx.io.bytestring.ByteString
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.secrets.*
import elide.secrets.SecretUtils.hashKey
import elide.secrets.dto.persisted.EncryptionMode
import elide.secrets.dto.persisted.Profile.Companion.get
import elide.secrets.dto.persisted.UserKey
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Implementation of [Secrets].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class SecretsImpl(private val encryption: Encryption, private val files: FileManagement) : Secrets {
  private var _initialized: Boolean = false
  override val initialized: Boolean
    get() = _initialized

  private val logger: Logger = Logging.of(SecretsImpl::class)
  private var passphraseOverride: String? = null

  override suspend fun init(path: Path, manifest: ElidePackageManifest?) {
    SecretsState.init(false, SecretUtils.path(path), manifest)
    if (files.metadataExists() && files.localExists()) {
      SecretsState.metadata = files.readMetadata()
      SecretsState.userKey =
        when (SecretsState.metadata.localEncryption) {
          EncryptionMode.PASSPHRASE -> {
            val pass =
              passphraseOverride
                ?: System.getenv(SecretValues.PASSPHRASE_ENVIRONMENT_VARIABLE)
                ?: throw IllegalStateException(SecretValues.PASSPHRASE_READ_EXCEPTION)
            UserKey(pass.hashKey(encryption))
          }
          EncryptionMode.GPG -> UserKey(SecretsState.metadata.fingerprint!!)
        }
      SecretsState.local = files.readLocal()
      val profileName = System.getenv(SecretValues.PROFILE_OVERRIDE_ENVIRONMENT_VARIABLE) ?: manifest?.secrets?.profile
      profileName?.let { loadProfile(it) }
      _initialized = true
    } else logger.debug(SecretValues.SECRETS_NOT_INITIALIZED_WARNING)
  }

  override fun listProfiles(): Set<String> = SecretsState.metadata.profiles.keys

  override fun loadProfile(profile: String) {
    if (profile !in SecretsState.metadata.profiles)
      throw IllegalArgumentException(SecretValues.profileDoesNotExistException(profile))
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

  internal fun resetInitialized() {
    _initialized = false
  }
}
