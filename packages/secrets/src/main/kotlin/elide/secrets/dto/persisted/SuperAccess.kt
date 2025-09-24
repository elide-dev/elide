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

import elide.secrets.Utils
import kotlinx.serialization.Serializable

/**
 * Remote access keys for secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class SuperAccess(
  val keys: Map<String, SecretKey>,
  val access: Map<String, Pair<UserKey, Set<String>>>,
) : Named {
  override val name = "super"

  init {
    Utils.checkNames(keys, "Key")
  }

  fun addKey(key: SecretKey): SuperAccess = copy(keys = keys + (key.name to key))

  fun removeKey(name: String): SuperAccess =
    copy(
      keys = keys - name,
      access =
        access.mapValues { (_, pair) -> if (name in pair.second) pair.copy(second = pair.second - name) else pair },
    )

  fun addAccess(name: String, key: UserKey): SuperAccess = copy(access = access + (name to (key to setOf())))

  fun removeAccess(name: String): SuperAccess = copy(access = access - name)

  fun addToAccess(name: String, profile: String) =
    copy(
      access =
        access.mapValues { (accessName, pair) ->
          if (accessName == name) pair.copy(second = pair.second + profile) else pair
        }
    )

  fun removeFromAccess(name: String, profile: String) =
    copy(
      access =
        access.mapValues { (accessName, pair) ->
          if (accessName == name) pair.copy(second = pair.second - profile) else pair
        }
    )
}
