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

import kotlinx.io.bytestring.ByteString
import elide.secrets.dto.persisted.LocalMetadata
import elide.secrets.dto.persisted.LocalProfile
import elide.secrets.dto.persisted.SecretKey
import elide.secrets.dto.persisted.SecretProfile

/** @author Lauri Heino <datafox> */
internal interface FileManagement {
  fun metadataExists(): Boolean

  fun readMetadata(): LocalMetadata

  fun writeMetadata(): ByteString

  fun localExists(): Boolean

  fun readLocal(): LocalProfile

  fun writeLocal(): ByteString

  fun canDecryptLocal(passphrase: String): Boolean

  fun profileExists(profile: String): Boolean

  fun readProfile(profile: String): Pair<SecretProfile, SecretKey>

  fun writeProfile(profile: SecretProfile, key: SecretKey): ByteString

  fun writeProfileBytes(profile: String, data: ByteString)

  fun writeKey(key: SecretKey)

  fun removeProfile(profile: String)

  fun profileBytes(profile: String): ByteString

  fun readKey(profile: String): SecretKey
}
