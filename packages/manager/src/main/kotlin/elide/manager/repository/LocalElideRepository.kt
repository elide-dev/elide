package elide.manager.repository

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import elide.manager.DownloadCompletedEvent
import elide.manager.DownloadProgressEvent
import elide.manager.DownloadStartEvent
import elide.manager.ElideInstallEvent

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
          if (progressCounter == PROGRESS_INTERVAL) {
            progress?.emit(DownloadProgressEvent(read.toFloat() / size.toFloat()))
            progressCounter = 0
          }
          source.readAtMostTo(buffer, BUFFER)
          read += buffer.transferTo(sink)
          progressCounter++
        }
        progress?.emit(DownloadCompletedEvent)
      }
    }
  }

  override fun close() = Unit

  companion object {
    private const val BUFFER = 1024 * 1024L
    private const val PROGRESS_INTERVAL = 20
  }
}
