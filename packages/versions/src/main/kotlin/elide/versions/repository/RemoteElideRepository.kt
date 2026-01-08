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

import elide.versions.DownloadCompletedEvent
import elide.versions.DownloadProgressEvent
import elide.versions.DownloadStartEvent
import elide.versions.ElideInstallEvent
import elide.versions.VersionsValues.INSTALL_IO_BUFFER
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Sink

/**
 * Remote implementation of [StandardElideRepository]. [catalogPath] is an HTTPS address.
 *
 * @author Lauri Heino <datafox>
 */
internal class RemoteElideRepository(catalogPath: String, val client: HttpClient = defaultHttpClient()) :
  StandardElideRepository(catalogPath), ElideRepository {
  override suspend fun getVersionCatalog(): ElideVersionCatalog =
    client.get(this@RemoteElideRepository.catalogPath).body<ElideVersionCatalog>()

  override suspend fun streamFile(path: String, sink: Sink, progress: FlowCollector<ElideInstallEvent>?) {
    val actualPath = if (path.matches(HTTP_REGEX)) path else "${catalogPath.substringBeforeLast('/')}/$path"
    client.prepareGet(actualPath).execute { response ->
      val size = response.contentLength() ?: -1L
      val channel: ByteReadChannel = response.body()
      progress?.emit(DownloadStartEvent)
      sink.use {
        var read = 0L
        while (!channel.exhausted()) {
          progress?.emit(DownloadProgressEvent(read.toFloat() / size.toFloat()))
          read += channel.readRemaining(INSTALL_IO_BUFFER).transferTo(sink)
        }
        progress?.emit(DownloadCompletedEvent)
      }
    }
  }

  override fun close() {
    client.close()
  }

  companion object {
    val HTTP_REGEX = "https?://.+".toRegex()

    fun defaultHttpClient(): HttpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }
  }
}
