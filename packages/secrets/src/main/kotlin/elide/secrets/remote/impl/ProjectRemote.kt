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
package elide.secrets.remote.impl

import elide.secrets.Utils
import elide.secrets.Utils.exists
import elide.secrets.Utils.read
import elide.secrets.Utils.write
import elide.secrets.Values
import elide.secrets.remote.Remote
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import elide.secrets.Utils.delete

/** @author Lauri Heino <datafox> */
internal class ProjectRemote(private val path: Path) : Remote {
  override val writeAccess: Boolean = true

  override suspend fun getMetadata(): ByteString? = getFile(Values.METADATA_FILE)

  override suspend fun getProfile(profile: String): ByteString? = getFile(Utils.profileName(profile))

  override suspend fun getAccess(access: String): ByteString? = getFile(Utils.accessName(access))

  override suspend fun getSuperAccess(): ByteString? = getFile(Values.SUPER_ACCESS_FILE)

  override suspend fun update(metadata: ByteString, profiles: Map<String, ByteString>) {
    writeFile(Values.METADATA_FILE, metadata)
    profiles.forEach { writeFile(it.key, it.value) }
  }

  override suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>,
    deletedProfiles: Set<String>,
  ) {
    writeFile(Values.METADATA_FILE, metadata)
    profiles.forEach { writeFile(Utils.profileName(it.key), it.value) }
    writeFile(Values.SUPER_ACCESS_FILE, superAccess)
    access.forEach { writeFile(Utils.accessName(it.key), it.value) }
    deletedProfiles.forEach { deleteFile(Utils.profileName(it)) }
  }

  private fun getFile(path: String): ByteString? = Path(this.path, path).run { if (exists()) read() else null }

  private fun writeFile(path: String, data: ByteString) {
    data.write(Path(this.path, path))
  }

  private fun deleteFile(path: String) {
    Path(this.path, path).delete()
  }
}
