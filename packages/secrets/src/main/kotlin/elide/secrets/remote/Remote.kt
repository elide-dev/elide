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
package elide.secrets.remote

import kotlinx.io.bytestring.ByteString

/**
 * Remote connection handling for secrets.
 *
 * @property writeAccess `true` if writing to this remote is permitted.
 * @author Lauri Heino <datafox>
 */
internal interface Remote {
  val writeAccess: Boolean

  suspend fun getMetadata(): ByteString?

  suspend fun getProfile(profile: String): ByteString?

  suspend fun getAccess(access: String): ByteString?

  suspend fun getSuperAccess(): ByteString?

  suspend fun update(metadata: ByteString, profiles: Map<String, ByteString>)

  suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>,
    deletedProfiles: Set<String>,
    deletedAccesses: Set<String>,
  )
}
