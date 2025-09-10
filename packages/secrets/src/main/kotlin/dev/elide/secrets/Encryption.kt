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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

/**
 * Cryptography functions for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal interface Encryption {
  /** Encrypts [data] with [key] using AES. */
  fun encryptAES(key: ByteString, data: ByteString): ByteString

  /** Decrypts [encrypted] with [key] using AES. */
  fun decryptAES(key: ByteString, encrypted: ByteString): ByteString

  /** Hashes [data] into a valid AES key using SHA-256. */
  fun hashKeySHA256(data: ByteString): ByteString

  /** Hashes [data] using SHA-1. */
  fun hashDataSHA1(data: ByteString): ByteString

  /** Encrypts [data] with the public key of [id] using GPG. */
  fun encryptGPG(id: String, data: ByteString): ByteString

  /** Decrypts [data] with the private key of [id] using GPG. */
  fun decryptGPG(id: String, encrypted: ByteString): ByteString

  /**
   * Hashes [data] using SHA-1, prefixing the data with `blob `, the size of [data] in bytes and a null byte. This is
   * how Git hashes files (blobs).
   */
  fun hashGitDataSHA1(data: ByteString): ByteString =
    hashDataSHA1(
      buildByteString {
        append("blob ${data.size}\u0000".encodeToByteArray())
        append(data)
      }
    )
}
