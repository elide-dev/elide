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

  /**
   * Returns the bytes of the remote's json-encoded [elide.secrets.dto.persisted.RemoteMetadata] file, or `null` if none
   * is present.
   */
  suspend fun getMetadata(): ByteString?

  /**
   * Returns the bytes of the remote's encrypted and cbor-encoded [elide.secrets.dto.persisted.ProfileMetadata] file
   * with the specified name, or `null` if none is present.
   */
  suspend fun getProfile(profile: String): ByteString?

  /**
   * Returns the bytes of the remote's encrypted and cbor-encoded [elide.secrets.dto.persisted.SecretAccess] file with
   * the specified name, or `null` if none is present.
   */
  suspend fun getAccess(access: String): ByteString?

  /**
   * Returns the bytes of the remote's encrypted and cbor-encoded [elide.secrets.dto.persisted.SuperAccess] file, or
   * `null` if none is present.
   */
  suspend fun getSuperAccess(): ByteString?

  /**
   * Updates files on the remote. This function may only be called to update a single access file's existing profiles,
   * and the profile keys must have not been changed. Incorrect use can corrupt a remote.
   *
   * @param metadata json-encoded bytes of updated [elide.secrets.dto.persisted.RemoteMetadata]
   * @param profiles names of profiles mapped to encrypted cbor-encoded bytes of updated
   * [elide.secrets.dto.persisted.ProfileMetadata]
   */
  suspend fun update(metadata: ByteString, profiles: Map<String, ByteString>)

  /**
   * Updates all files on the remote. Incorrect use can corrupt a remote.
   *
   * @param metadata json-encoded bytes of updated [elide.secrets.dto.persisted.RemoteMetadata]
   * @param profiles names of profiles mapped to encrypted cbor-encoded bytes of updated
   * [elide.secrets.dto.persisted.ProfileMetadata]
   * @param superAccess encrypted cbor-encoded bytes of updated [elide.secrets.dto.persisted.SuperAccess]
   * @param deletedProfiles names of profiles to be deleted from the remote
   * @param deletedAccesses names of access files to be deleted from the remote
   */
  suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>,
    deletedProfiles: Set<String>,
    deletedAccesses: Set<String>,
  )
}
