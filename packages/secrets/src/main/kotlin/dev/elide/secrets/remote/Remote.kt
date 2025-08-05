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
package dev.elide.secrets.remote

import dev.elide.secrets.dto.persisted.SecretMetadata
import kotlinx.io.bytestring.ByteString

/**
 * Remote connection handling for secrets.
 *
 * @property writeAccess `true` if writing to this remote is permitted.
 * @author Lauri Heino <datafox>
 */
internal interface Remote {
  val writeAccess: Boolean

  /** Returns the metadata file from this remote, or `null` if none was present. */
  suspend fun getMetadata(): ByteString?

  /** Returns the passphrase validator file from this remote, or `null` if none was present. */
  suspend fun getValidator(): ByteString?

  /** Returns a key and collection file for [profile] in a pair from this remote. */
  suspend fun getCollection(profile: String): Pair<ByteString, ByteString>

  /** Removes a key and collection file for [profile] from this remote */
  suspend fun removeCollection(profile: String)

  /** Initializes this remote with [metadata] and passphrase [validator]. */
  suspend fun init(metadata: SecretMetadata, validator: ByteString)

  /**
   * Updates [collections] on this remote. The map keys are profile names, and the pairs contain encrypted key and
   * collection data.
   */
  suspend fun update(collections: Map<String, Pair<ByteString, ByteString>>)

  companion object {
    const val REMOTE_NAME = "remote"
    const val PASSPHRASE_NAME = "passphrase"
  }
}
