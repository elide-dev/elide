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
package elide.secrets.dto.persisted

import elide.secrets.impl.ByteStringSerializer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.hexToByteString
import kotlinx.serialization.Serializable

/**
 * Encryption key for secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class UserKey(
  val mode: EncryptionMode,
  @Serializable(with = ByteStringSerializer::class) val key: ByteString,
) {
  constructor(hashedPassphrase: ByteString) : this(EncryptionMode.PASSPHRASE, hashedPassphrase)

  @OptIn(ExperimentalStdlibApi::class)
  constructor(gpgFingerprint: String) : this(EncryptionMode.GPG, gpgFingerprint.hexToByteString())
}
