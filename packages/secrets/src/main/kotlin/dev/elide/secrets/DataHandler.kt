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
package dev.elide.secrets

import dev.elide.secrets.dto.persisted.SecretCollection
import dev.elide.secrets.dto.persisted.SecretMetadata
import kotlinx.io.bytestring.ByteString

/**
 * Data handling functions for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal interface DataHandler {
  /** Returns `true` if the metadata file exists. */
  suspend fun metadataExists(): Boolean

  /** Reads and deserializes the metadata file. */
  suspend fun readMetadata(): SecretMetadata

  /** Serializes [metadata] and writes it to the metadata file. */
  suspend fun writeMetadata(metadata: SecretMetadata)

  /** Deserializes [data] to metadata. */
  suspend fun deserializeMetadata(data: ByteString): SecretMetadata

  /** Serializes [metadata]. */
  suspend fun serializeMetadata(metadata: SecretMetadata): ByteString

  /** Returns `true` if the local collection file exists. */
  suspend fun localExists(): Boolean

  /** Reads, decrypts and deserializes the local collection file with hashed [passphrase]. */
  suspend fun readLocal(passphrase: ByteString): SecretCollection

  /** Serializes [local], encrypts it with hashed [passphrase] and writes it to the local collection file. */
  suspend fun writeLocal(passphrase: ByteString, local: SecretCollection)

  /** Returns `true` if a collection file for [profile] exists. */
  suspend fun collectionExists(profile: String): Boolean

  /**
   * Reads, decrypts and deserializes a collection file for [profile]. Decryption is done by reading an encrypted key
   * with [readKey] and decrypting it with the hashed [passphrase].
   */
  suspend fun readCollection(profile: String, passphrase: ByteString): SecretCollection

  /** Reads an encrypted collection file for [profile]. */
  suspend fun readEncryptedCollection(profile: String): ByteString

  /**
   * Serializes [collection], encrypts it and writes it to a collection file for [profile]. Encryption is done by
   * reading an encrypted key with [readKey] and decrypting it with the hashed [passphrase].
   */
  suspend fun writeCollection(profile: String, passphrase: ByteString, collection: SecretCollection): String

  /** Deletes a collection file and a key file for [profile]. */
  suspend fun deleteCollection(profile: String)

  /** Decrypts [encrypted] collection with [key] and deserializes it. */
  suspend fun decryptCollection(key: ByteString, encrypted: ByteString): SecretCollection

  /** Serializes [collection] and encrypts it with [key]. */
  suspend fun encryptCollection(key: ByteString, collection: SecretCollection): ByteString

  /** Returns `true` if a key file for [profile] exists. */
  suspend fun keyExists(profile: String): Boolean

  /** Reads and decrypts a key file for [profile] with the hashed [passphrase]. */
  suspend fun readKey(profile: String, passphrase: ByteString): ByteString

  /** Encrypts [key] with the hashed [passphrase] and writes it to a key file for [profile]. */
  suspend fun writeKey(profile: String, passphrase: ByteString, key: ByteString)

  /** Decrypts an [encrypted] key with the hashed [passphrase]. */
  suspend fun decryptKey(passphrase: ByteString, encrypted: ByteString): ByteString

  /** Encrypts [key] with the hashed [passphrase]. */
  suspend fun encryptKey(passphrase: ByteString, key: ByteString): ByteString

  /** Creates remote passphrase validator data from [metadata] and hashed remote [passphrase] */
  suspend fun createValidator(metadata: SecretMetadata, passphrase: ByteString): ByteString

  /** Validates a remote passphrase [validator] with the [metadata] and hashed remote [passphrase] */
  suspend fun validate(metadata: SecretMetadata, passphrase: ByteString, validator: ByteString): Boolean
}
