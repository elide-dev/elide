package elide.manager.repository

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
import elide.manager.DownloadCompletedEvent
import elide.manager.DownloadProgressEvent
import elide.manager.DownloadStartEvent
import elide.manager.ElideInstallEvent

/**
 * Remote implementation of [StandardElideRepository]. [catalogPath] is an HTTPS address.
 *
 * @author Lauri Heino <datafox>
 */
internal class RemoteElideRepository(
  catalogPath: String,
  val client: HttpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }
) : StandardElideRepository(catalogPath), ElideRepository {
  override suspend fun getVersionCatalog(): ElideVersionCatalog =
    client.get(this@RemoteElideRepository.catalogPath).body<ElideVersionCatalog>()

  override suspend fun streamFile(path: String, sink: Sink, progress: FlowCollector<ElideInstallEvent>?) {
    val actualPath =
      if (path.matches("https?://.+".toRegex())) path else "${catalogPath.substringBeforeLast('/')}/$path"
    client.prepareGet(actualPath).execute { response ->
      val size = response.contentLength() ?: -1L
      val channel: ByteReadChannel = response.body()
      progress?.emit(DownloadStartEvent)
      sink.use {
        var read = 0L
        while (!channel.exhausted()) {
          progress?.emit(DownloadProgressEvent(read.toFloat() / size.toFloat()))
          read += channel.readRemaining(BUFFER).transferTo(sink)
        }
        progress?.emit(DownloadCompletedEvent)
      }
    }
  }

  override fun close() {
    client.close()
  }

  companion object {
    private const val BUFFER = 1024 * 1024L
  }
}
