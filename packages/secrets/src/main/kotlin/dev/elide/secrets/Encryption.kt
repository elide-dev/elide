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

/**
 * Cryptography functions for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal interface Encryption {
    /** Encrypts [data] with [key]. */
    fun encrypt(key: ByteString, data: ByteString): ByteString

    /** Decrypts [encrypted] with [key]. */
    fun decrypt(key: ByteString, encrypted: ByteString): ByteString

    /** Cryptographically hashes [passphrase] into a valid key. */
    fun hash(passphrase: String): ByteString
}
