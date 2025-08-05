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
package dev.elide.secrets.dto.persisted

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.Serializable

/**
 * Collection for [Secrets][Secret].
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class SecretCollection(val secrets: Map<String, Secret<*>>) {
  public constructor() : this(emptyMap())

  init {
    secrets.forEach {
      if (it.key != it.value.name)
        throw IllegalStateException("Secret name ${it.value.name} does not match map key ${it.key}")
    }
  }

  public fun add(secret: Secret<*>): SecretCollection {
    return copy(secrets = secrets + (secret.name to secret))
  }

  public inline operator fun <reified T> get(name: String): T? {
    return if (T::class == ByteString::class) {
      (secrets[name]?.value as? ByteArray)?.let { ByteString(it) as T }
    } else {
      secrets[name]?.value as? T
    }
  }
}
