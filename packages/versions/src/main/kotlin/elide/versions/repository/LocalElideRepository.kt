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
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import elide.versions.DownloadCompletedEvent
import elide.versions.DownloadProgressEvent
import elide.versions.DownloadStartEvent
import elide.versions.ElideInstallEvent
import elide.versions.VersionsValues.INSTALL_IO_BUFFER
import elide.versions.VersionsValues.INSTALL_PROGRESS_INTERVAL

/**
 * Local implementation of [StandardElideRepository]. [catalogPath] is a local absolute file path.
 *
 * @author Lauri Heino <datafox>
 */
internal class LocalElideRepository(catalogPath: String) : StandardElideRepository(catalogPath), ElideRepository {
  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun getVersionCatalog(): ElideVersionCatalog =
    Json.Default.decodeFromSource<ElideVersionCatalog>(SystemFileSystem.source(Path(catalogPath)).buffered())

  override suspend fun streamFile(path: String, sink: Sink, progress: FlowCollector<ElideInstallEvent>?) {
    val actualPath = Path(path).let { if (it.isAbsolute) it else Path(Path(catalogPath).parent!!, path) }
    val size = SystemFileSystem.metadataOrNull(actualPath)!!.size
    sink.use { sink ->
      SystemFileSystem.source(actualPath).buffered().use { source ->
        var read = 0L
        val buffer = Buffer()
        progress?.emit(DownloadStartEvent)
        var progressCounter = 0
        while (!source.exhausted()) {
          if (progressCounter == INSTALL_PROGRESS_INTERVAL) {
            progress?.emit(DownloadProgressEvent(read.toFloat() / size.toFloat()))
            progressCounter = 0
          }
          source.readAtMostTo(buffer, INSTALL_IO_BUFFER)
          read += buffer.transferTo(sink)
          progressCounter++
        }
        progress?.emit(DownloadCompletedEvent)
      }
    }
  }

  override fun close() = Unit
}
