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
package dev.elide.secrets.impl

import dev.elide.secrets.*
import dev.elide.secrets.dto.persisted.SecretCollection
import dev.elide.secrets.dto.persisted.SecretMetadata
import kotlinx.io.buffered
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteString
import kotlinx.io.write
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import elide.annotations.Inject
import elide.annotations.Singleton

/**
 * Implementation of [DataHandler], using [Json] for metadata and [Cbor] for collections.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class DataHandlerImpl
@Inject
internal constructor(private val encryption: Encryption, private val json: Json, private val cbor: BinaryFormat) :
  DataHandler {
  override suspend fun metadataExists(): Boolean = SystemFileSystem.exists(metadataPath())

  override suspend fun readMetadata(): SecretMetadata {
    val data = SystemFileSystem.source(metadataPath()).buffered().use { it.readByteString() }
    return deserializeMetadata(data)
  }

  override suspend fun writeMetadata(metadata: SecretMetadata) {
    val data = serializeMetadata(metadata)
    SystemFileSystem.sink(metadataPath()).buffered().use { it.write(data) }
  }

  override suspend fun deserializeMetadata(data: ByteString): SecretMetadata =
    json.decodeFromString(data.decodeToString(Charsets.UTF_8))

  override suspend fun serializeMetadata(metadata: SecretMetadata): ByteString =
    json.encodeToString(metadata).encodeToByteString(Charsets.UTF_8)

  override suspend fun localExists(): Boolean = SystemFileSystem.exists(localPath())

  override suspend fun readLocal(passphrase: ByteString): SecretCollection {
    val encrypted = SystemFileSystem.source(localPath()).buffered().use { it.readByteString() }
    val data = encryption.decrypt(passphrase, encrypted)
    return cbor.decodeFromByteArray(data.toByteArray())
  }

  override suspend fun writeLocal(passphrase: ByteString, local: SecretCollection) {
    val data = ByteString(cbor.encodeToByteArray(local))
    val encrypted = encryption.encrypt(passphrase, data)
    SystemFileSystem.sink(localPath()).buffered().use { it.write(encrypted) }
  }

  override suspend fun collectionExists(profile: String): Boolean = SystemFileSystem.exists(collectionPath(profile))

  override suspend fun readCollection(profile: String, passphrase: ByteString): SecretCollection {
    val key = readKey(profile, passphrase)
    val encrypted = readEncryptedCollection(profile)
    return decryptCollection(key, encrypted)
  }

  override suspend fun readEncryptedCollection(profile: String): ByteString =
    SystemFileSystem.source(collectionPath(profile)).buffered().use { it.readByteString() }

  override suspend fun writeCollection(profile: String, passphrase: ByteString, collection: SecretCollection): String {
    val key = readKey(profile, passphrase)
    val encrypted = encryptCollection(key, collection)
    SystemFileSystem.sink(collectionPath(profile)).buffered().use { it.write(encrypted) }
    return Utils.sha(encrypted)
  }

  override suspend fun deleteCollection(profile: String) {
    SystemFileSystem.delete(collectionPath(profile))
    SystemFileSystem.delete(keyPath(profile))
  }

  override suspend fun decryptCollection(key: ByteString, encrypted: ByteString): SecretCollection {
    val data = encryption.decrypt(key, encrypted)
    return cbor.decodeFromByteArray(data.toByteArray())
  }

  override suspend fun encryptCollection(key: ByteString, collection: SecretCollection): ByteString {
    val data = ByteString(cbor.encodeToByteArray(collection))
    return encryption.encrypt(key, data)
  }

  override suspend fun keyExists(profile: String): Boolean = SystemFileSystem.exists(keyPath(profile))

  override suspend fun readKey(profile: String, passphrase: ByteString): ByteString {
    val encrypted = SystemFileSystem.source(keyPath(profile)).buffered().use { it.readByteString() }
    return decryptKey(passphrase, encrypted)
  }

  override suspend fun writeKey(profile: String, passphrase: ByteString, key: ByteString) {
    val encrypted = encryptKey(passphrase, key)
    SystemFileSystem.sink(keyPath(profile)).buffered().use { it.write(encrypted) }
  }

  override suspend fun decryptKey(passphrase: ByteString, encrypted: ByteString): ByteString {
    return encryption.decrypt(passphrase, encrypted)
  }

  override suspend fun encryptKey(passphrase: ByteString, key: ByteString): ByteString {
    return encryption.encrypt(passphrase, key)
  }

  override suspend fun createValidator(metadata: SecretMetadata, passphrase: ByteString): ByteString {
    val data = encryption.hash(metadata.name + metadata.organization)
    val encrypted = encryption.encrypt(passphrase, data)
    return encrypted
  }

  override suspend fun validate(metadata: SecretMetadata, passphrase: ByteString, validator: ByteString): Boolean {
    val expected = encryption.hash(metadata.name + metadata.organization)
    val actual = encryption.decrypt(passphrase, validator)
    return expected == actual
  }

  private suspend fun metadataPath(): Path = Path(SecretsState.Companion.get().path, Values.METADATA_NAME)

  private suspend fun localPath(): Path = Path(SecretsState.Companion.get().path, Values.LOCAL_COLLECTION_NAME)

  private suspend fun collectionPath(profile: String): Path =
    Path(SecretsState.Companion.get().path, Utils.collectionName(profile))

  private suspend fun keyPath(profile: String): Path = Path(SecretsState.Companion.get().path, Utils.keyName(profile))
}
