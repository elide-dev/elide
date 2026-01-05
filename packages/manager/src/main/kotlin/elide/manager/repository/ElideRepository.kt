package elide.manager.repository

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import elide.manager.ElideInstallEvent
import elide.manager.ElideVersionDto

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
