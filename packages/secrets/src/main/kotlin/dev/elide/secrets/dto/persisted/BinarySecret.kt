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

import dev.elide.secrets.Utils
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.Serializable

/**
 * [Secret] containing binary data.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class BinarySecret(override val name: String, override val value: ByteArray) : Secret<ByteArray> {

  public constructor(name: String, value: ByteString) : this(name, value.toByteArray())

  init {
    Utils.checkName(name, "Secret")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BinarySecret) return false

    if (name != other.name) return false
    if (!value.contentEquals(other.value)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + value.contentHashCode()
    return result
  }
}
