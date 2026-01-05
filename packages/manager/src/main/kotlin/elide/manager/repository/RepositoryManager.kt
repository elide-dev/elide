package elide.manager.repository

import kotlinx.io.files.Path

/**
* @author Lauri Heino <datafox>
*/
public interface RepositoryManager {
  public fun createLocalCatalog(directory: Path, relativePaths: Boolean): String

  public fun createRemoteCatalog(directory: Path, root: String): String
}
