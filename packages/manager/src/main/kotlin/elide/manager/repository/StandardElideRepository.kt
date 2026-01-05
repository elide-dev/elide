package elide.manager.repository

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.io.Sink
import elide.manager.ElideInstallEvent
import elide.manager.ElideVersionDto

/**
 * Abstract class for standard [repositories][ElideRepository]. A standard repository has an [ElideVersionCatalog] at
 * [catalogPath].
 *
 * @author Lauri Heino <datafox>
 */
internal abstract class StandardElideRepository(val catalogPath: String) : ElideRepository {
  /** Returns the [ElideVersionCatalog] at [catalogPath]. */
  protected abstract suspend fun getVersionCatalog(): ElideVersionCatalog

  /** Streams a file from [path] to [sink] while emitting [progress] updates. */
  protected abstract suspend fun streamFile(path: String, sink: Sink, progress: FlowCollector<ElideInstallEvent>?)

  override suspend fun getVersions(): List<ElideVersionDto> =
    getVersionCatalog().versions.flatMap { (version, systems) ->
      systems.platforms.map { (platform, _) -> ElideVersionDto(version, platform) }
    }

  override suspend fun getFile(
    version: ElideVersionDto,
    extension: String,
    sink: Sink,
    progress: FlowCollector<ElideInstallEvent>?,
  ) {
    val versionData =
      requireNotNull(getVersionCatalog().versions[version.version]) {
        "The version ${version.version} does not exist in repository"
      }
    val path =
      requireNotNull(versionData.platforms[version.platform]) {
        "The version ${version.version} does not exist in repository"
      }
    streamFile("$path.$extension", sink, progress)
  }
}
