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

import kotlinx.serialization.Serializable
import elide.secrets.Utils

/**
 * Metadata for secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class RemoteMetadata(
  override val name: String,
  override val profiles: Map<String, ProfileMetadata>,
  val superAccess: AccessMetadata,
  val access: Map<String, AccessMetadata>,
) : SecretMetadata {

  init {
    Utils.checkName(name, "Project")
    Utils.checkNames(profiles, "Profile")
    Utils.checkNames(access, "Access")
  }

  fun add(profile: ProfileMetadata): RemoteMetadata {
    return copy(profiles = profiles + (profile.name to profile))
  }

  fun add(access: AccessMetadata): RemoteMetadata {
    return copy(access = this.access + (access.name to access))
  }
}
