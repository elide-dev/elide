package elide.manager.repository

import elide.annotations.Singleton
import elide.manager.ElideInstallConfig

/**
 * Factory for creating [ElideRepository] instances from [ElideInstallConfig.repositories].
 *
 * @author Lauri Heino <datafox>
 */
public interface ElideRepositoryFactory {
  /**
   * Id should be in format `type:path` where `type` is the type of repository and `path` is a path to
   * [catalog.json][ElideVersionCatalog] in that repository's format.
   *
   * Currently supported repository types are:
   * - [local][LocalElideRepository]: path is an absolute path on your local filesystem.
   * - [remote][RemoteElideRepository]: path is a url starting with `https://`.
   */
  public fun get(id: String): ElideRepository
}

@Singleton
internal class ElideRepositoryFactoryImpl() : ElideRepositoryFactory {
  override fun get(id: String): ElideRepository {
    val splitIndex = id.indexOf(':')
    if (splitIndex == -1) throw IllegalArgumentException("Type must be separated from path with a colon (:)")
    val path = id.substring(splitIndex + 1)
    return when (val type = id.substring(0, splitIndex)) {
      "local" -> LocalElideRepository(path)
      "remote" -> RemoteElideRepository(path)
      else -> throw IllegalArgumentException("Unknown repository type: $type")
    }
  }
}
