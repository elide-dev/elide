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
package elide.versions.repository

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import elide.versions.ElideInstallEvent
import elide.versions.ElideVersionDto

/**
 * API for repositories serving versions of Elide.
 *
 * @author Lauri Heino <datafox>
 */
public interface ElideRepository {
  public suspend fun getVersions(): List<ElideVersionDto>

  public suspend fun getFile(
    version: ElideVersionDto,
    extension: String,
    sink: Sink,
    progress: FlowCollector<ElideInstallEvent>? = null
  )

  public fun close()
}

/** Should only be used for small files like hashes or signatures. */
public suspend fun ElideRepository.getFile(version: ElideVersionDto, extension: String): ByteString =
  Buffer().also { getFile(version, extension, it) }.readByteString()
