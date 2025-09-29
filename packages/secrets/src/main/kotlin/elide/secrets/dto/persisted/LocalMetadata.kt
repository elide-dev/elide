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

import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.Serializable
import elide.secrets.Utils

/**
 * Metadata for locally stored secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class LocalMetadata(
  override val name: String,
  override val profiles: Map<String, ProfileMetadata>,
  val localEncryption: EncryptionMode,
  val fingerprint: String? = null,
) : SecretMetadata {
  @OptIn(ExperimentalStdlibApi::class)
  constructor(
    name: String,
    key: UserKey,
    vararg profiles: ProfileMetadata,
  ) : this(
    name,
    profiles.associateBy { it.name },
    key.mode,
    if (key.mode == EncryptionMode.GPG) key.key.toHexString() else null,
  )

  init {
    Utils.checkName(name, "Project")
    Utils.checkNames(profiles, "Profile")
  }

  fun add(profile: ProfileMetadata): LocalMetadata {
    return copy(profiles = profiles + (profile.name to profile))
  }

  fun remove(profile: String): LocalMetadata {
    return copy(profiles = profiles - profile)
  }

  fun removeAll(profiles: Iterable<String>): LocalMetadata {
    return copy(profiles = this.profiles - profiles)
  }
}
