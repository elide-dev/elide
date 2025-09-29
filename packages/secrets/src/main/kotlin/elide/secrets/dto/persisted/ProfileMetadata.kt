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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.Serializable
import elide.secrets.Utils

/**
 * Metadata for a [Profile].
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class ProfileMetadata(override val name: String, val hash: String) : Named {
  @OptIn(ExperimentalStdlibApi::class)
  constructor(
    name: String,
    hash: ByteString,
  ) : this(name, hash.toHexString())

  init {
    Utils.checkName(name, "Profile")
  }
}
