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
package elide.secrets

import com.github.kinquirer.core.Choice
import java.security.SecureRandom
import kotlinx.io.buffered
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteString
import kotlinx.io.write
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlin.io.path.absolutePathString
import elide.secrets.dto.persisted.EncryptionMode
import elide.secrets.dto.persisted.Named
import elide.secrets.dto.persisted.SecretKey
import elide.secrets.dto.persisted.UserKey

/**
 * Internal utilities for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal object Utils {
  /** Generates [size] bytes. */
  fun generateBytes(size: Int): ByteString = ByteString(ByteArray(size).apply { SecureRandom().nextBytes(this) })

  /** Throws an [IllegalArgumentException] if a profile name is invalid (is empty or contains whitespace). */
  fun checkName(name: String, type: String) {
    if (name.isEmpty() || ' ' in name) throw IllegalArgumentException("$type name must not be empty or contain spaces")
  }

  fun <T : Named> checkNames(map: Map<String, T>, type: String) =
    map.forEach {
      if (it.key != it.value.name)
        throw IllegalStateException("$type name ${it.value.name} does not match map key ${it.key}")
    }

  inline fun <reified T> T.serialize(serializer: BinaryFormat): ByteString =
    ByteString(serializer.encodeToByteArray(this))

  inline fun <reified T> ByteString.deserialize(serializer: BinaryFormat): T =
    serializer.decodeFromByteArray(toByteArray())

  inline fun <reified T> T.serialize(serializer: Json): ByteString =
    serializer.encodeToString(this).encodeToByteString()

  inline fun <reified T> ByteString.deserialize(serializer: Json): T = serializer.decodeFromString(decodeToString())

  fun ByteString.encrypt(key: SecretKey, encryption: Encryption): ByteString = encryption.encryptAES(key.key, this)

  fun ByteString.decrypt(key: SecretKey, encryption: Encryption): ByteString = encryption.decryptAES(key.key, this)

  @OptIn(ExperimentalStdlibApi::class)
  fun ByteString.encrypt(key: UserKey, encryption: Encryption): ByteString =
    when (key.mode) {
      EncryptionMode.PASSPHRASE -> encryption.encryptAES(key.key, this)
      EncryptionMode.GPG -> encryption.encryptGPG(key.key.toHexString(), this)
    }

  @OptIn(ExperimentalStdlibApi::class)
  fun ByteString.decrypt(key: UserKey, encryption: Encryption): ByteString =
    when (key.mode) {
      EncryptionMode.PASSPHRASE -> encryption.decryptAES(key.key, this)
      EncryptionMode.GPG -> encryption.decryptGPG(key.key.toHexString(), this)
    }

  fun String.hashKey(encryption: Encryption): ByteString = encryption.hashKeySHA256(encodeToByteString())

  fun Path.exists(): Boolean = SystemFileSystem.exists(this)

  fun Path.read(): ByteString = SystemFileSystem.source(this).buffered().use { it.readByteString() }

  fun ByteString.write(path: Path): ByteString {
    SystemFileSystem.sink(path).buffered().use { it.write(this) }
    return this
  }

  fun Path.delete() = SystemFileSystem.delete(this)

  fun passphrase(): String? = System.getenv(Values.PASSPHRASE_ENVIRONMENT_VARIABLE)

  fun profileName(profile: String): String = "${Values.PROFILE_FILE_PREFIX}$profile${Values.PROFILE_FILE_EXTENSION}"

  fun keyName(profile: String): String = "${Values.PROFILE_FILE_PREFIX}$profile${Values.KEY_FILE_EXTENSION}"

  fun accessName(access: String): String = "$access${Values.ACCESS_FILE_EXTENSION}"

  inline fun <T> Collection<T>.choices(block: T.() -> String): List<Choice<T>> = map { Choice(it.block(), it) }

  @JvmName("namedChoices")
  inline fun <T : Named> Collection<T>.choices(block: T.() -> String = { name }): List<Choice<T>> = map {
    Choice(it.block(), it)
  }

  inline fun <T> Map<String, T>.choices(block: T.(String) -> String = { it }): List<Choice<T>> = map {
    Choice(it.value.block(it.key), it.value)
  }

  fun defaultPath(): Path = Path(System.getProperty("user.dir"), Values.DEFAULT_PATH)

  fun path(path: java.nio.file.Path): Path = Path(path.absolutePathString(), Values.DEFAULT_PATH)
}
