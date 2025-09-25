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
 * Collection for [Secrets][Secret].
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class SecretProfile(override val name: String, override val secrets: Map<String, Secret<*>>) : Profile {
  constructor(name: String) : this(name, emptyMap())

  init {
    Utils.checkName(name, "Profile")
    Utils.checkNames(secrets, "Secret")
  }

  fun add(secret: Secret<*>): SecretProfile {
    return copy(secrets = secrets + (secret.name to secret))
  }

  fun addAll(vararg secrets: Secret<*>): SecretProfile {
    return copy(secrets = this@SecretProfile.secrets + secrets.associateBy { it.name })
  }

  fun remove(name: String): SecretProfile {
    return copy(secrets = secrets - name)
  }
}
